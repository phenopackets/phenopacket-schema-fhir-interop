package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.PhenoPacket;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class FhirConverterTest {

    @Test
    void testPatientOnlyBundle() throws IOException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
//        bundle.setTimestamp(Date.from(Instant.now()));

        Patient patient = new Patient();
        patient.setId("PATIENT#1");

        bundle.addEntry().setResource(patient);

//        System.out.println(FhirContext.forR4().newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));

        PhenoPacket converted = new FhirConverter().toPhenoPacket(bundle);

        assertThat(converted.hasPatient(), is(true));
    }

    @Test
    void testTorontoExampleBundle() throws IOException {
//        System.out.println(FhirContext.forR4().newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));

//        PhenoPacket converted = new FhirConverter().toPhenoPacket(TestExamples.rareDiseaseBundle());
//        System.out.println(PhenopacketFormat.toYaml(converted));
//
//        assertThat(converted.hasPatient(), is(true));
//        Individual patient = converted.getPatient();
//        assertThat(patient.getId(), equalTo(TestExamples.rareDiseasePhenoPacket().getPatient().getId()));
//        assertThat(patient.getPhenotypesList(), equalTo(TestExamples.rareDiseasePhenoPacket().getPatient().getPhenotypesList()));
    }
}