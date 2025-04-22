package app.quantun.simpleapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the home page.
 */
@Controller
public class HomeController {

    /**
     * /**
     * Redirect to Swagger UI documentation.
     *
     * @return the name of the view to render
     */
    @GetMapping("/api-docs-ui")
    public String apiDocs() {
        return "redirect:/swagger-ui.html";
    }
}
