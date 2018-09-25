package org.phenopackets.schema.v1.fhir.interop.converters;

import com.google.common.collect.ImmutableMap;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.core.OntologyClass;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class ConceptMapperTest {

    @Test
    void setUpFromMap() {
        Map<String, String> curieMappings = ImmutableMap.of("HP", "http://purl.obolibrary.org/obo/HP_");

        ConceptMapper instance = ConceptMapper.fromMap(curieMappings);

        OntologyClass hpoOntologyClass = OntologyClass.newBuilder().setId("HP:0012828").setLabel("Severe").build();
        CodeableConcept result = instance.toCodeableConcept(hpoOntologyClass);
        assertTrue(ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/hp.owl", "HP:0012828", "Severe").equalsDeep(result));
    }

    @Test
    void testToOntologyClassHpoConcept() {
        CodeableConcept codeableConcept = ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/hp.owl", "HP:0012828", "Severe");

        OntologyClass expected = OntologyClass.newBuilder().setId("HP:0012828").setLabel("Severe").build();
        ConceptMapper instance = ConceptMapper.fromMap(Collections.emptyMap());

        assertThat(instance.toOntologyClass(codeableConcept), equalTo(expected));
    }

}