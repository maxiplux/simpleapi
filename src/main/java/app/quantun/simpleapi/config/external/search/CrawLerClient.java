package app.quantun.simpleapi.config.external.search;

import app.quantun.simpleapi.model.contract.request.ProductRequest;
import app.quantun.simpleapi.model.contract.response.ProductResponse;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;



@HttpExchange(url = "/api", headers = {"Accept=*/*"})
public interface CrawLerClient {

    @GetExchange(value = "/messages/ids")
    String getDocuments();



    @PostExchange(value = "/documents", contentType = "application/xml")
    String search(@RequestBody String request);
}
