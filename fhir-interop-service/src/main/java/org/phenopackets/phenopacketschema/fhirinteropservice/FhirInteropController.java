package org.phenopackets.phenopacketschema.fhirinteropservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.fhir.interop.converters.FhirConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
@RestController
public class FhirInteropController {

    private static final Logger logger = LoggerFactory.getLogger(FhirInteropController.class);
    private final IParser jsonParser;

    public FhirInteropController() {
        this.jsonParser = FhirContext.forR4().newJsonParser();
    }

    @CrossOrigin
    @PostMapping(value = "convert",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public Phenopacket convertToPhenoPacket(@RequestBody String input) {
        logger.debug("Received input {}", input);
        Bundle bundle = (Bundle) jsonParser.parseResource(input);
        return new FhirConverter().toPhenopacket(bundle);
    }

}
