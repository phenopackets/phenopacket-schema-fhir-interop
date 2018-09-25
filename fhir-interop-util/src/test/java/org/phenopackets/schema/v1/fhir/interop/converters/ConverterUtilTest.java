package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.core.Phenotype;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class ConverterUtilTest {

    @Test
    void makePhenotypeEmptyCondition() {
        Phenotype result = ConverterUtil.makePhenotype(new Condition());
        assertThat(result, equalTo(Phenotype.getDefaultInstance()));
    }

    @Test
    void makePhenotypeConditionWithNoCode() {
        Condition condition = new Condition();
        // need a code in order to make a sensible phenotype
        condition.setSubject(new Reference(new Patient()));
        Phenotype result = ConverterUtil.makePhenotype(condition);
        assertThat(result, equalTo(Phenotype.getDefaultInstance()));
    }
}