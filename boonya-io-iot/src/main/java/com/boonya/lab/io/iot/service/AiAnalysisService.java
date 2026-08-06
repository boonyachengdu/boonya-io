package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.dto.DeviceDiagnosisDTO;
import com.boonya.lab.io.iot.dto.TrendPredictionDTO;
import com.boonya.lab.io.iot.model.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 智能分析服务
 *
 * 当前实现基于规则与统计的智能分析（不依赖外部大模型 API，避免网络/编译问题）。
 * 后续可在此扩展接入大模型（如 Spring AI / 通义千问），保留接口契约不变即可平滑替换。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final TimeSeriesService timeSeriesService;

    /** 温度异常上限阈值（℃） */
    private static final double TEMP_HIGH_THRESHOLD = 30.0;
    /** 温度异常下限阈值（℃） */
    private static final double TEMP_LOW_THRESHOLD = 5.0;
    /** 标准差异常倍数：超过 mean + N*std 视为突变 */
    private static final double STD_ANOMALY_FACTOR = 3.0;

    /**
     * 设备异常诊断
     * 查询最近 24 小时数据，计算统计指标（均值、标准差、最大最小值），
     * 基于阈值判断是否存在异常，返回诊断报告
     */
    public DeviceDiagnosisDTO diagnoseDevice(String deviceId) {
        long endTs = System.currentTimeMillis();
        long startTs = endTs - 24L * 60 * 60 * 1000; // 最近 24 小时

        List<DeviceData> history = timeSeriesService.queryHistory(deviceId, startTs, endTs);

        if (history == null || history.isEmpty()) {
            return DeviceDiagnosisDTO.noData(deviceId, "未查询到设备最近24小时的历史数据，无法进行诊断");
        }

        // 计算统计指标
        double[] stats = computeStats(history);
        double mean = stats[0];
        double std = stats[1];
        double max = stats[2];
        double min = stats[3];

        DeviceDiagnosisDTO.Statistics statistics = new DeviceDiagnosisDTO.Statistics(
                history.size(), round(mean), round(std), round(max), round(min), round(max - min));

        // 异常判断
        List<String> anomalies = new ArrayList<>();
        if (max > TEMP_HIGH_THRESHOLD) {
            anomalies.add(String.format("温度过高: 最大值 %.2f℃ 超过阈值 %.2f℃", max, TEMP_HIGH_THRESHOLD));
        }
        if (min < TEMP_LOW_THRESHOLD) {
            anomalies.add(String.format("温度过低: 最小值 %.2f℃ 低于阈值 %.2f℃", min, TEMP_LOW_THRESHOLD));
        }
        if (std > 0 && (max - mean) > STD_ANOMALY_FACTOR * std) {
            anomalies.add(String.format("存在异常突变: 最大值 %.2f℃ 偏离均值超过 %.1f 倍标准差", max, STD_ANOMALY_FACTOR));
        }
        if (std > 0 && (mean - min) > STD_ANOMALY_FACTOR * std) {
            anomalies.add(String.format("存在异常下跌: 最小值 %.2f℃ 偏离均值超过 %.1f 倍标准差", min, STD_ANOMALY_FACTOR));
        }

        String status = anomalies.isEmpty() ? "NORMAL" : "ABNORMAL";

        // 建议
        List<String> suggestions = new ArrayList<>();
        if (anomalies.isEmpty()) {
            suggestions.add("设备运行状态正常，建议保持现有监控策略");
        } else {
            suggestions.add("建议检查设备运行环境及传感器状态");
            if (max > TEMP_HIGH_THRESHOLD) {
                suggestions.add("温度过高，建议加强散热或降低负载");
            }
            if (min < TEMP_LOW_THRESHOLD) {
                suggestions.add("温度过低，建议加强保温措施");
            }
            if (std > 0 && ((max - mean) > STD_ANOMALY_FACTOR * std || (mean - min) > STD_ANOMALY_FACTOR * std)) {
                suggestions.add("数据存在突变，建议检查是否存在突发故障或外部干扰");
            }
        }

        log.info("Device {} diagnosis completed: status={}, anomalies={}", deviceId, status, anomalies.size());

        return new DeviceDiagnosisDTO(
                deviceId, System.currentTimeMillis(), status, null, history.size(),
                statistics, anomalies, anomalies.size(), suggestions);
    }

    /**
     * 温度趋势预测
     * 基于最近历史数据的移动平均趋势，预测未来 N 分钟的温度范围
     */
    public TrendPredictionDTO predictTrend(String deviceId, int minutes) {
        long endTs = System.currentTimeMillis();
        // 取最近 2 小时数据作为预测样本
        long startTs = endTs - 2L * 60 * 60 * 1000;

        List<DeviceData> history = timeSeriesService.queryHistory(deviceId, startTs, endTs);

        if (history == null || history.size() < 2) {
            return TrendPredictionDTO.noData(deviceId, minutes, "历史数据不足，无法进行趋势预测");
        }

        double[] stats = computeStats(history);
        double mean = stats[0];
        double std = stats[1];

        // 计算线性回归斜率（最小二乘法）
        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = history.get(i).getValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // 预测未来值（以最后一个点为起点）
        double lastValue = history.get(history.size() - 1).getValue();
        // 假设历史数据平均间隔（毫秒）
        long avgIntervalMs = (endTs - startTs) / Math.max(n - 1, 1);
        long predictStepMs = avgIntervalMs > 0 ? avgIntervalMs : 60_000L;
        int predictSteps = (int) Math.max(1, (minutes * 60_000L) / predictStepMs);

        double predictedValue = lastValue + slope * predictSteps;
        // 预测区间：均值 ± 标准差
        double lowerBound = predictedValue - std;
        double upperBound = predictedValue + std;

        // 趋势判断
        String trend;
        if (Math.abs(slope) < 1e-4) {
            trend = "STABLE";
        } else if (slope > 0) {
            trend = "RISING";
        } else {
            trend = "FALLING";
        }

        TrendPredictionDTO.PredictedValue predicted = new TrendPredictionDTO.PredictedValue(
                round(predictedValue), round(lowerBound), round(upperBound));

        // 风险提示
        List<String> risks = new ArrayList<>();
        if (predictedValue > TEMP_HIGH_THRESHOLD) {
            risks.add(String.format("预测未来 %d 分钟温度可能超过高温阈值 %.2f℃", minutes, TEMP_HIGH_THRESHOLD));
        }
        if (predictedValue < TEMP_LOW_THRESHOLD) {
            risks.add(String.format("预测未来 %d 分钟温度可能低于低温阈值 %.2f℃", minutes, TEMP_LOW_THRESHOLD));
        }

        log.info("Device {} trend prediction: trend={}, predicted={}, minutes={}",
                deviceId, trend, round(predictedValue), minutes);

        return new TrendPredictionDTO(
                deviceId, minutes, System.currentTimeMillis(),
                "OK", null, predicted, trend, round(slope), round(lastValue), risks);
    }

    /**
     * 计算统计指标：均值、标准差、最大值、最小值
     */
    private double[] computeStats(List<DeviceData> data) {
        double sum = 0, sumSq = 0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (DeviceData d : data) {
            double v = d.getValue();
            sum += v;
            sumSq += v * v;
            if (v > max) max = v;
            if (v < min) min = v;
        }
        int n = data.size();
        double mean = sum / n;
        double variance = (sumSq / n) - (mean * mean);
        double std = Math.sqrt(Math.max(0, variance));
        return new double[]{mean, std, max, min};
    }

    /**
     * 保留两位小数
     */
    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
