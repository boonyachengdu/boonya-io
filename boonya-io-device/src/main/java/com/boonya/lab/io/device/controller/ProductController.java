package com.boonya.lab.io.device.controller;

import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.device.entity.Product;
import com.boonya.lab.io.device.entity.ThingModel;
import com.boonya.lab.io.device.entity.ThingService;
import com.boonya.lab.io.device.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "产品与物模型管理", description = "产品 CRUD、物模型属性/服务管理接口")
public class ProductController {

    private final ProductService productService;

    // ==================== 产品 CRUD ====================

    @GetMapping("/api/products")
    @Operation(summary = "分页查询产品列表")
    public Result<PageResult<Product>> queryProducts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String productKey) {
        return Result.success(productService.queryProducts(pageNum, pageSize, productName, productKey));
    }

    @GetMapping("/api/products/all")
    @Operation(summary = "获取所有启用的产品（下拉选择用）")
    public Result<List<Product>> listAllEnabled() {
        return Result.success(productService.listAllEnabled());
    }

    @PostMapping("/api/products")
    @Operation(summary = "创建产品")
    public Result<Product> createProduct(@RequestBody Product product) {
        return Result.success(productService.createProduct(product));
    }

    @GetMapping("/api/products/{productKey}")
    @Operation(summary = "获取产品详情")
    public Result<Product> getProduct(@PathVariable String productKey) {
        return Result.success(productService.getProduct(productKey));
    }

    @PutMapping("/api/products/{productKey}")
    @Operation(summary = "更新产品")
    public Result<Product> updateProduct(@PathVariable String productKey,
                                          @RequestBody Product product) {
        return Result.success(productService.updateProduct(productKey, product));
    }

    @DeleteMapping("/api/products/{productKey}")
    @Operation(summary = "删除产品", description = "同时删除关联的物模型属性和服务")
    public Result<Void> deleteProduct(@PathVariable String productKey) {
        productService.deleteProduct(productKey);
        return Result.success();
    }

    // ==================== 物模型属性 ====================

    @GetMapping("/api/products/{productKey}/properties")
    @Operation(summary = "获取物模型属性列表")
    public Result<List<ThingModel>> getProperties(@PathVariable String productKey) {
        return Result.success(productService.getProperties(productKey));
    }

    @PostMapping("/api/products/{productKey}/properties")
    @Operation(summary = "添加物模型属性")
    public Result<ThingModel> addProperty(@PathVariable String productKey,
                                           @RequestBody ThingModel property) {
        return Result.success(productService.addProperty(productKey, property));
    }

    @PutMapping("/api/thing-model/properties/{id}")
    @Operation(summary = "更新物模型属性")
    public Result<ThingModel> updateProperty(@PathVariable Long id,
                                              @RequestBody ThingModel property) {
        return Result.success(productService.updateProperty(id, property));
    }

    @DeleteMapping("/api/thing-model/properties/{id}")
    @Operation(summary = "删除物模型属性")
    public Result<Void> deleteProperty(@PathVariable Long id) {
        productService.deleteProperty(id);
        return Result.success();
    }

    // ==================== 物模型服务 ====================

    @GetMapping("/api/products/{productKey}/services")
    @Operation(summary = "获取物模型服务列表")
    public Result<List<ThingService>> getServices(@PathVariable String productKey) {
        return Result.success(productService.getServices(productKey));
    }

    @PostMapping("/api/products/{productKey}/services")
    @Operation(summary = "添加物模型服务")
    public Result<ThingService> addService(@PathVariable String productKey,
                                            @RequestBody ThingService service) {
        return Result.success(productService.addService(productKey, service));
    }

    @PutMapping("/api/thing-model/services/{id}")
    @Operation(summary = "更新物模型服务")
    public Result<ThingService> updateService(@PathVariable Long id,
                                               @RequestBody ThingService service) {
        return Result.success(productService.updateService(id, service));
    }

    @DeleteMapping("/api/thing-model/services/{id}")
    @Operation(summary = "删除物模型服务")
    public Result<Void> deleteService(@PathVariable Long id) {
        productService.deleteService(id);
        return Result.success();
    }
}
