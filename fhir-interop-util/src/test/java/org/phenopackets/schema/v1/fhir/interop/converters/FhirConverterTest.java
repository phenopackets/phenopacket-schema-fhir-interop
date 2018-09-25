package org.phenopackets.schema.v1.fhir.interop.converters;

import com.google.protobuf.Timestamp;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.PhenoPacket;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.core.Phenotype;
import org.phenopackets.schema.v1.core.Resource;
import org.phenopackets.schema.v1.io.PhenoPacketFormat;

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

        PhenoPacket converted = new FhirConverter().toPhenoPacket(bundle);

        assertThat(converted, equalTo(PhenoPacket.getDefaultInstance()));
    }

    @Test
    void testPatientOnlyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = new Patient();
        patient.setId("PATIENT#1");

        bundle.addEntry().setResource(patient);

        PhenoPacket converted = new FhirConverter().toPhenoPacket(bundle);

        assertThat(converted.hasPatient(), is(true));
    }

    @Test
    void testTwoPatientBundle() throws IOException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = new Patient();
        patient.setId("PATIENT#1");
        bundle.addEntry().setResource(patient);

        Patient secondPatient = new Patient();
        secondPatient.setId("PATIENT#2");
        bundle.addEntry().setResource(secondPatient);

        PhenoPacket converted = new FhirConverter().toPhenoPacket(bundle);
        System.out.println(PhenoPacketFormat.toYaml(converted));
        assertThat(converted.hasPatient(), is(false));
        assertThat(converted.getIndividualsCount(), equalTo(2));
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

        Phenotype phenotype1 = Phenotype.newBuilder()
                .setType(ConverterUtil.ontologyClass("a:1", "a wibble"))
                .setSeverity(ConverterUtil.ontologyClass("b:1", "b wibble"))
                .build();

        Condition condition2 = new Condition();
        condition2.setCode(ConverterUtil.codeableConcept("a.url", "a:2", "a frood"));
        condition2.setSeverity(ConverterUtil.codeableConcept("b.url", "b:2", "b frood"));
        // Fhir has oneof datetime, Age, Period, String - For this example we're going to use a string
//        condition2.setOnset(new StringType(""));
        condition2.setSubject(new Reference(patient));

        Phenotype phenotype2 = Phenotype.newBuilder()
                .setType(ConverterUtil.ontologyClass("a:2", "a frood"))
                .setSeverity(ConverterUtil.ontologyClass("b:2", "b frood"))
                .build();


        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition1);
        bundle.addEntry().setResource(condition2);


        Individual individual = Individual.newBuilder()
                .setId("PATIENT#1")
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(now.getEpochSecond()).build())
                .setSex(ConverterUtil.ontologyClass("PATO:0000384", "male"))
                .addPhenotypes(phenotype1)
                .addPhenotypes(phenotype2)
                .build();

        MetaData metaData = MetaData.newBuilder()
                .setCreatedBy("FHIR converter")
                .addResources(Resource.newBuilder()
                        .setId("pato")
                        .setNamespacePrefix("PATO")
                        .setUrl("http://purl.obolibrary.org/obo/pato.owl")
                        .setVersion("2018-08-14")
                        .setName("Phenotype And Trait Ontology")
                        .build())
                .addResources(Resource.newBuilder().setId("a").setNamespacePrefix("a").setUrl("a.url").build())
                .addResources(Resource.newBuilder().setId("b").setNamespacePrefix("b").setUrl("b.url").build())
                .build();

        PhenoPacket expected = PhenoPacket.newBuilder()
                .setPatient(individual)
                .setMetaData(metaData)
                .build();

        PhenoPacket converted = new FhirConverter().toPhenoPacket(bundle);
        System.out.println(PhenoPacketFormat.toYaml(converted));
        assertThat(converted.getPatient(), equalTo(expected.getPatient()));
        // metadata contains a created timestamp which might not always agree and make the test fail unpredictably
        // so just test the resources and created by
        assertThat(converted.getMetaData().getCreatedBy(), equalTo(expected.getMetaData().getCreatedBy()));
        assertThat(converted.getMetaData().getResourcesList(), equalTo(expected.getMetaData().getResourcesList()));
    }
}