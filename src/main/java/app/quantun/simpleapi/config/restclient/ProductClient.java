package app.quantun.simpleapi.config.restclient;

import app.quantun.simpleapi.model.contract.request.ProductRequest;
import app.quantun.simpleapi.model.contract.response.ProductResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ProductClient {
    @PostExchange
    ProductResponse addProduct(@RequestBody ProductRequest request);
}
