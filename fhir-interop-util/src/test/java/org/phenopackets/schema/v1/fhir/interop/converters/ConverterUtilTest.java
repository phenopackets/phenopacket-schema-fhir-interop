package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.core.PhenotypicFeature;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class ConverterUtilTest {

    @Test
    void makePhenotypeEmptyCondition() {
        PhenotypicFeature result = ConverterUtil.makePhenotypicFeature(new Condition());
        assertThat(result, equalTo(PhenotypicFeature.getDefaultInstance()));
    }

    @Test
    void makePhenotypeConditionWithNoCode() {
        Condition condition = new Condition();
        // need a code in order to make a sensible phenotype
        condition.setSubject(new Reference(new Patient()));
        PhenotypicFeature result = ConverterUtil.makePhenotypicFeature(condition);
        assertThat(result, equalTo(PhenotypicFeature.getDefaultInstance()));
    }
}