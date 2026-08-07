package com.boonya.lab.io.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.common.exception.BusinessException;
import com.boonya.lab.io.common.exception.ResourceNotFoundException;
import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.device.entity.Product;
import com.boonya.lab.io.device.entity.ThingModel;
import com.boonya.lab.io.device.entity.ThingService;
import com.boonya.lab.io.device.mapper.ProductMapper;
import com.boonya.lab.io.device.mapper.ThingModelMapper;
import com.boonya.lab.io.device.mapper.ThingServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ThingModelMapper thingModelMapper;
    private final ThingServiceMapper thingServiceMapper;

    // ==================== 产品 CRUD ====================

    public PageResult<Product> queryProducts(int pageNum, int pageSize, String productName, String productKey) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productName)) {
            wrapper.like(Product::getProductName, productName);
        }
        if (StringUtils.hasText(productKey)) {
            wrapper.eq(Product::getProductKey, productKey);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public List<Product> listAllEnabled() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getEnabled, true);
        return productMapper.selectList(wrapper);
    }

    public Product getProduct(String productKey) {
        Product product = findByProductKey(productKey);
        if (product == null) {
            throw new ResourceNotFoundException("Product", productKey);
        }
        return product;
    }

    @Transactional
    public Product createProduct(Product product) {
        if (!StringUtils.hasText(product.getProductKey())) {
            product.setProductKey(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        if (productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductKey, product.getProductKey())) != null) {
            throw new BusinessException("产品Key已存在: " + product.getProductKey());
        }
        if (!StringUtils.hasText(product.getNodeType())) {
            product.setNodeType("DIRECT");
        }
        if (!StringUtils.hasText(product.getProtocolType())) {
            product.setProtocolType("MQTT");
        }
        if (!StringUtils.hasText(product.getDataFormat())) {
            product.setDataFormat("JSON");
        }
        if (product.getEnabled() == null) {
            product.setEnabled(true);
        }
        productMapper.insert(product);
        log.info("产品创建成功: productKey={}, name={}", product.getProductKey(), product.getProductName());
        return product;
    }

    @Transactional
    public Product updateProduct(String productKey, Product product) {
        Product existing = getProduct(productKey);
        if (StringUtils.hasText(product.getProductName())) {
            existing.setProductName(product.getProductName());
        }
        if (StringUtils.hasText(product.getNodeType())) {
            existing.setNodeType(product.getNodeType());
        }
        if (StringUtils.hasText(product.getProtocolType())) {
            existing.setProtocolType(product.getProtocolType());
        }
        if (StringUtils.hasText(product.getDataFormat())) {
            existing.setDataFormat(product.getDataFormat());
        }
        if (product.getDescription() != null) {
            existing.setDescription(product.getDescription());
        }
        if (product.getEnabled() != null) {
            existing.setEnabled(product.getEnabled());
        }
        productMapper.updateById(existing);
        log.info("产品更新: productKey={}", productKey);
        return existing;
    }

    @Transactional
    public void deleteProduct(String productKey) {
        Product product = getProduct(productKey);
        // 删除关联的物模型属性和服务
        thingModelMapper.delete(new LambdaQueryWrapper<ThingModel>()
                .eq(ThingModel::getProductKey, productKey));
        thingServiceMapper.delete(new LambdaQueryWrapper<ThingService>()
                .eq(ThingService::getProductKey, productKey));
        productMapper.deleteById(product.getId());
        log.info("产品删除: productKey={}", productKey);
    }

    // ==================== 物模型属性 ====================

    public List<ThingModel> getProperties(String productKey) {
        // 验证产品存在
        getProduct(productKey);
        return thingModelMapper.selectList(new LambdaQueryWrapper<ThingModel>()
                .eq(ThingModel::getProductKey, productKey)
                .orderByAsc(ThingModel::getSort));
    }

    @Transactional
    public ThingModel addProperty(String productKey, ThingModel property) {
        Product product = getProduct(productKey);
        property.setProductKey(product.getProductKey());

        // 检查 identifier 是否重复
        Long count = thingModelMapper.selectCount(new LambdaQueryWrapper<ThingModel>()
                .eq(ThingModel::getProductKey, productKey)
                .eq(ThingModel::getIdentifier, property.getIdentifier()));
        if (count > 0) {
            throw new BusinessException("属性标识已存在: " + property.getIdentifier());
        }

        if (!StringUtils.hasText(property.getAccessMode())) {
            property.setAccessMode("RW");
        }
        if (property.getSort() == null) {
            property.setSort(0);
        }
        thingModelMapper.insert(property);
        log.info("物模型属性添加: productKey={}, identifier={}", productKey, property.getIdentifier());
        return property;
    }

    @Transactional
    public ThingModel updateProperty(Long id, ThingModel property) {
        ThingModel existing = thingModelMapper.selectById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("ThingModel", String.valueOf(id));
        }
        if (StringUtils.hasText(property.getName())) {
            existing.setName(property.getName());
        }
        if (StringUtils.hasText(property.getDataType())) {
            existing.setDataType(property.getDataType());
        }
        if (property.getUnit() != null) {
            existing.setUnit(property.getUnit());
        }
        if (property.getMinValue() != null) {
            existing.setMinValue(property.getMinValue());
        }
        if (property.getMaxValue() != null) {
            existing.setMaxValue(property.getMaxValue());
        }
        if (StringUtils.hasText(property.getAccessMode())) {
            existing.setAccessMode(property.getAccessMode());
        }
        if (property.getDescription() != null) {
            existing.setDescription(property.getDescription());
        }
        if (property.getSort() != null) {
            existing.setSort(property.getSort());
        }
        thingModelMapper.updateById(existing);
        log.info("物模型属性更新: id={}", id);
        return existing;
    }

    @Transactional
    public void deleteProperty(Long id) {
        ThingModel property = thingModelMapper.selectById(id);
        if (property == null) {
            throw new ResourceNotFoundException("ThingModel", String.valueOf(id));
        }
        thingModelMapper.deleteById(id);
        log.info("物模型属性删除: id={}, identifier={}", id, property.getIdentifier());
    }

    // ==================== 物模型服务 ====================

    public List<ThingService> getServices(String productKey) {
        getProduct(productKey);
        return thingServiceMapper.selectList(new LambdaQueryWrapper<ThingService>()
                .eq(ThingService::getProductKey, productKey)
                .orderByAsc(ThingService::getId));
    }

    @Transactional
    public ThingService addService(String productKey, ThingService service) {
        Product product = getProduct(productKey);
        service.setProductKey(product.getProductKey());

        Long count = thingServiceMapper.selectCount(new LambdaQueryWrapper<ThingService>()
                .eq(ThingService::getProductKey, productKey)
                .eq(ThingService::getIdentifier, service.getIdentifier()));
        if (count > 0) {
            throw new BusinessException("服务标识已存在: " + service.getIdentifier());
        }

        if (!StringUtils.hasText(service.getCallType())) {
            service.setCallType("ASYNC");
        }
        thingServiceMapper.insert(service);
        log.info("物模型服务添加: productKey={}, identifier={}", productKey, service.getIdentifier());
        return service;
    }

    @Transactional
    public ThingService updateService(Long id, ThingService service) {
        ThingService existing = thingServiceMapper.selectById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("ThingService", String.valueOf(id));
        }
        if (StringUtils.hasText(service.getName())) {
            existing.setName(service.getName());
        }
        if (StringUtils.hasText(service.getCallType())) {
            existing.setCallType(service.getCallType());
        }
        if (service.getInputParams() != null) {
            existing.setInputParams(service.getInputParams());
        }
        if (service.getOutputParams() != null) {
            existing.setOutputParams(service.getOutputParams());
        }
        if (service.getDescription() != null) {
            existing.setDescription(service.getDescription());
        }
        thingServiceMapper.updateById(existing);
        log.info("物模型服务更新: id={}", id);
        return existing;
    }

    @Transactional
    public void deleteService(Long id) {
        ThingService service = thingServiceMapper.selectById(id);
        if (service == null) {
            throw new ResourceNotFoundException("ThingService", String.valueOf(id));
        }
        thingServiceMapper.deleteById(id);
        log.info("物模型服务删除: id={}, identifier={}", id, service.getIdentifier());
    }

    // ==================== 内部方法 ====================

    private Product findByProductKey(String productKey) {
        return productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductKey, productKey));
    }
}
