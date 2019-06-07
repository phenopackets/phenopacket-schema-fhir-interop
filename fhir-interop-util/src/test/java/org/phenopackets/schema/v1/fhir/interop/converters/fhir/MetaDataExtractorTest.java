package org.phenopackets.schema.v1.fhir.interop.converters.fhir;

import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.core.MetaData;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class MetaDataExtractorTest {

    @Test
    void createdBy() {
        MetaData metaData = MetaDataExtractor.builder().createdBy("Nemo").buildMetaData();
        assertThat(metaData.getCreatedBy(), equalTo("Nemo"));
    }
}