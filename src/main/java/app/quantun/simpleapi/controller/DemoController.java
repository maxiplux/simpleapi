package app.quantun.simpleapi.controller;

import app.quantun.simpleapi.model.contract.response.AuthResponse;
import app.quantun.simpleapi.model.contract.response.ProductResponse;
import app.quantun.simpleapi.service.impl.AuthService;
import app.quantun.simpleapi.service.impl.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DemoController {

    private final AuthService authService;
    private final ProductService productService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestParam String username,
                                              @RequestParam String password,
                                              @RequestParam(defaultValue = "30") Integer expiresInMins) {
        AuthResponse response = authService.login(username, password, expiresInMins);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> addProduct(
            @RequestParam String title,
            @RequestParam String body) {
        ProductResponse response = productService.addProduct(title, body);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/demo")
    public ResponseEntity<ProductResponse> demoFlow() {
        // First authenticate


        // Then add a product using the token
        ProductResponse productResponse = productService.addProduct(
                "Amor Y Paz",
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        );
        return ResponseEntity.ok(productResponse);
    }
}
