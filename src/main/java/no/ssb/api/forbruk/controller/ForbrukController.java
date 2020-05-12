package no.ssb.api.forbruk.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.ssb.api.forbruk.service.ForbrukService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by rsa on 29.04.2019.
 */
@Api("forbruk-api")
@RestController
@RequestMapping("")
public class ForbrukController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String HEADER_APIKEY = "api_key";

    @Value("${api-keys}")
    private List<String> acceptedApiKeys;

    @Value("${forbruk.filepath.codes}")
    private String productCodesFilePath;

    @Value("${forbruk.filepath.result}")
    private String resultFilePath;

    @Value("${forbruk.filepath.result.fileprefix}")
    private String resultFilePrefix;

    @Value("${forbruk.filepath.result.filepostfix}")
    private String resultFilePostfix;

    @Autowired
    ForbrukService forbrukService;

    @ApiOperation(value = "Hent produktinformasjon")
    @RequestMapping(value ="/produktinfo", method = RequestMethod.GET)
    public ResponseEntity<String> produktinfo (@RequestHeader HttpHeaders header) {
        log.info("header: {}", header);
        if (!authorizeRequest(header)) {
            return new ResponseEntity<>("Unauthorized. Api-key er ugyldig eller mangler", HttpStatus.UNAUTHORIZED);
        }
        log.info("kjør henting av forbruk-produkt");
        try {
            forbrukService.retrieveProductInformation(productCodesFilePath, resultFilePath, resultFilePrefix, resultFilePostfix);
            return new ResponseEntity<>("henting av forbruk-produkt ferdig", HttpStatus.OK);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean authorizeRequest(HttpHeaders header) {
        return header.get(HEADER_APIKEY) != null && acceptedApiKeys.contains(header.get(HEADER_APIKEY).get(0));
    }

}
