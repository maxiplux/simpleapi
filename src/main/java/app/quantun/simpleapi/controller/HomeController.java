package app.quantun.simpleapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home")
@Tag(name = "Home Controller", description = "Home Controller API")
public class HomeController {

    @GetMapping
    @Operation(summary = "Get home message")
    public String getHome() {
        return "Welcome to the Home API!";
    }

    @PostMapping
    @Operation(summary = "Create a new home message")
    public String createHome(@RequestBody String message) {
        return "Created message: " + message;
    }

    @PutMapping
    @Operation(summary = "Update the home message")
    public String updateHome(@RequestBody String message) {
        return "Updated message: " + message;
    }
}
