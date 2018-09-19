package org.phenopackets.schema.v1.fhir.interop.converters;

import com.google.common.collect.ImmutableMap;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.core.OntologyClass;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class OntologyClassConverterTest {

    @Test
    void setUpFromMap() {
        Map<String, String> curieMappings = ImmutableMap.of("HP", "http://purl.obolibrary.org/obo/HP_");

        OntologyClassConverter instance = OntologyClassConverter.fromMap(curieMappings);

        OntologyClass hpoOntologyClass = OntologyClass.newBuilder().setId("HP:0012828").setLabel("Severe").build();
        CodeableConcept result = instance.toCodeableConcept(hpoOntologyClass);
        assertTrue(ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/hp.owl", "HP:0012828", "Severe").equalsDeep(result));
    }

}