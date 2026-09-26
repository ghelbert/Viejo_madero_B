package com.example.mi_api.restaurant.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.mi_api.restaurant.dto.auth.LoginRequest;
import com.example.mi_api.restaurant.dto.order.CreateOrderRequest;
import com.example.mi_api.restaurant.dto.order.StatusRequest;
import com.example.mi_api.restaurant.dto.order.UpdateOrderItemsRequest;
import com.example.mi_api.restaurant.dto.product.ProductRequest;
import com.example.mi_api.restaurant.dto.user.UpdateUserRequest;
import com.example.mi_api.restaurant.dto.user.UserRequest;
import com.example.mi_api.restaurant.service.AuthService;
import com.example.mi_api.restaurant.service.OrderService;
import com.example.mi_api.restaurant.service.ProductService;
import com.example.mi_api.restaurant.service.TableService;
import com.example.mi_api.restaurant.service.UserService;

@RestController
@RequestMapping("/api")
public class RestaurantController {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantController.class);

    private final AuthService authService;
    private final TableService tableService;
    private final ProductService productService;
    private final UserService userService;
    private final OrderService orderService;

    public RestaurantController(
            AuthService authService,
            TableService tableService,
            ProductService productService,
            UserService userService,
            OrderService orderService) {
        this.authService = authService;
        this.tableService = tableService;
        this.productService = productService;
        this.userService = userService;
        this.orderService = orderService;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        logger.info("Login request received");
        return authService.login(request);
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> tables() {
        return tableService.findAll();
    }

    @GetMapping("/tables/{id}/detail")
    public Map<String, Object> tableDetail(@PathVariable long id) {
        return tableService.findDetail(id);
    }

    @GetMapping("/products")
    public List<Map<String, Object>> products() {
        return productService.findAvailable();
    }

    @GetMapping("/admin/products")
    public List<Map<String, Object>> allProducts() {
        return productService.findAll();
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return productService.findCategories();
    }

    @PostMapping("/products")
    public Map<String, Object> createProduct(@RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(@PathVariable long id, @RequestBody ProductRequest request) {
        productService.update(id, request);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return userService.findAll();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(@PathVariable long id, @RequestBody UpdateUserRequest request) {
        userService.update(id, request);
    }

    @PatchMapping("/users/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setUserActive(@PathVariable long id, @RequestParam boolean active) {
        userService.setActive(id, active);
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @PostMapping("/orders/{id}/confirm")
    public Map<String, Object> confirmOrder(@PathVariable long id, @RequestParam long userId) {
        return orderService.confirm(id, userId);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders(@RequestParam(required = false) String status) {
        return orderService.findAll(status);
    }

    @GetMapping("/orders/{id}/detail")
    public Map<String, Object> orderDetail(@PathVariable long id) {
        return orderService.findDetail(id);
    }

    @PostMapping("/orders/{id}/status")
    public Map<String, Object> updateOrderStatus(
            @PathVariable long id,
            @RequestBody StatusRequest request) {
        return orderService.updateStatus(id, request);
    }

    @PatchMapping("/orders/{id}/items")
    public Map<String, Object> updateOrderItems(
            @PathVariable long id,
            @RequestBody UpdateOrderItemsRequest request) {
        return orderService.updateItems(id, request);
    }

    @DeleteMapping("/orders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable long id) {
        orderService.delete(id);
    }
}
