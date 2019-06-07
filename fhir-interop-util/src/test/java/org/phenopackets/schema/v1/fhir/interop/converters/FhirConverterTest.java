package org.phenopackets.schema.v1.fhir.interop.converters;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.phenopackets.schema.v1.core.Resource;

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
    void noPatientBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Phenopacket converted = new FhirConverter().toPhenopacket(bundle);

        assertThat(converted, equalTo(Phenopacket.getDefaultInstance()));
    }

    @Test
    void testPatientOnlyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = new Patient();
        patient.setId("PATIENT#1");

        bundle.addEntry().setResource(patient);

        Phenopacket converted = new FhirConverter().toPhenopacket(bundle);

        assertThat(converted.hasSubject(), is(true));
    }

    @Test
    void testTwoPatientBundle() throws IOException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = new Patient();
        patient.setId("PATIENT#1");
        patient.setBirthDate(Date.from(Instant.parse("1972-12-03T10:15:30.00Z")));
        patient.addName(new HumanName().setText("Zaphod").setFamily("Beeblebrox"));
        bundle.addEntry().setResource(patient);

        Patient secondPatient = new Patient();
        secondPatient.setId("PATIENT#2");
        bundle.addEntry().setResource(secondPatient);

        // TODO: Should this throw an exception - too many patients? Or return the first?
        Phenopacket converted = new FhirConverter().toPhenopacket(bundle);
        System.out.println(JsonFormat.printer().print(converted));
        assertThat(converted.getSubject().getId(), equalTo("PATIENT#1"));
        assertThat(converted.getSubject().getDateOfBirth(), equalTo(Timestamp.newBuilder().setSeconds(Instant.parse("1972-12-03T10:15:30.00Z").getEpochSecond()).build()));
    }

    @Test
    void testPatientWithPhenotypes() throws IOException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Instant now = Instant.now();
        Patient patient = new Patient();
        patient.setId("PATIENT#1");
        patient.setBirthDate(new Date(now.toEpochMilli()));
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        Condition condition1 = new Condition();
        condition1.setCode(ConverterUtil.codeableConcept("a.url", "a:1", "a wibble"));
        condition1.setSeverity(ConverterUtil.codeableConcept("b.url", "b:1", "b wibble"));
        // Fhir has oneof datetime, Age, Period, String - For this example we're going to use a string
//        condition1.setOnset(new StringType(""));
        condition1.setSubject(new Reference(patient));

        PhenotypicFeature phenotype1 = PhenotypicFeature.newBuilder()
                .setType(ConverterUtil.ontologyClass("a:1", "a wibble"))
                .setSeverity(ConverterUtil.ontologyClass("b:1", "b wibble"))
                .build();

        Condition condition2 = new Condition();
        condition2.setCode(ConverterUtil.codeableConcept("a.url", "a:2", "a frood"));
        condition2.setSeverity(ConverterUtil.codeableConcept("b.url", "b:2", "b frood"));
        // Fhir has oneof datetime, Age, Period, String - For this example we're going to use a string
//        condition2.setOnset(new StringType(""));
        condition2.setSubject(new Reference(patient));

        PhenotypicFeature phenotype2 = PhenotypicFeature.newBuilder()
                .setType(ConverterUtil.ontologyClass("a:2", "a frood"))
                .setSeverity(ConverterUtil.ontologyClass("b:2", "b frood"))
                .build();


        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition1);
        bundle.addEntry().setResource(condition2);


        Individual individual = Individual.newBuilder()
                .setId("PATIENT#1")
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(now.getEpochSecond()).build())
                .setSex(Sex.MALE)
                .build();

        MetaData metaData = MetaData.newBuilder()
                .setCreatedBy("FHIR converter")
                .addResources(Resource.newBuilder().setId("a").setNamespacePrefix("a").setUrl("a.url").build())
                .addResources(Resource.newBuilder().setId("b").setNamespacePrefix("b").setUrl("b.url").build())
                .build();

        Phenopacket expected = Phenopacket.newBuilder()
                .setSubject(individual)
                .addPhenotypicFeatures(phenotype1)
                .addPhenotypicFeatures(phenotype2)
                .setMetaData(metaData)
                .build();

        Phenopacket converted = new FhirConverter().toPhenopacket(bundle);
        System.out.println(JsonFormat.printer().print(converted));
        assertThat(converted.getSubject(), equalTo(expected.getSubject()));
        // metadata contains a created timestamp which might not always agree and make the test fail unpredictably
        // so just test the resources and created by
        assertThat(converted.getMetaData().getCreatedBy(), equalTo(expected.getMetaData().getCreatedBy()));
        assertThat(converted.getMetaData().getResourcesList(), equalTo(expected.getMetaData().getResourcesList()));
    }
}