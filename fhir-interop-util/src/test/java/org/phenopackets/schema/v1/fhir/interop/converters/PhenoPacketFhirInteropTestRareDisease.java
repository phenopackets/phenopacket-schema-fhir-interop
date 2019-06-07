package org.phenopackets.schema.v1.fhir.interop.converters;

import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.Family;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil.codeableConcept;
import static org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil.ontologyClass;


/**
 * Driver tests to enable the alignment between Phenopackets and FHIR for rare disease cases.
 * 
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class PhenoPacketFhirInteropTestRareDisease {

    // the individual ID needs to match that used in the pedigree
    private static final String FAMILY_ID = "FAMILY:1";
    private static final String PROBAND_ID = "PROBAND:1";
    private static final String MOTHER_ID = "MOTHER:1";
    private static final String FATHER_ID = "FATHER:1";
    private static final String SISTER_ID = "SISTER:1";

    private static final PhenotypicFeature abnormalPhenotype = PhenotypicFeature.newBuilder()
            .setType(ontologyClass("HP:0000118", "Phenotypic abnormality"))
            .build();


    private static final OntologyClass FEMALE = ontologyClass("PATO:0000383", "female");
    private static final OntologyClass MALE = ontologyClass("PATO:0000384", "male");

    private static final CodeableConcept FHIR_FEMALE = ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/pato.owl", FEMALE.getId(), FEMALE.getLabel());
    private static final CodeableConcept FHIR_MALE = ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/pato.owl", MALE.getId(), MALE.getLabel());


    private static CodeableConcept codeableConcept(String system, String id, String label){
        return new CodeableConcept().addCoding(new Coding(system, id, label));
    }

    private static CodeableConcept hpoConcept(String id, String label) {
        return codeableConcept("http://purl.obolibrary.org/obo/hp.owl", id, label);
    }

    private Pedigree createPedigree() {
        Pedigree.Person mother = Pedigree.Person.newBuilder()
                .setFamilyId(FAMILY_ID)
                .setIndividualId(MOTHER_ID)
                .setSex(Sex.FEMALE)
                .setAffectedStatus(Pedigree.Person.AffectedStatus.AFFECTED)
                .build();

        Pedigree.Person father = Pedigree.Person.newBuilder()
                .setFamilyId(FAMILY_ID)
                .setIndividualId(FATHER_ID)
                .setSex(Sex.MALE)
                .setAffectedStatus(Pedigree.Person.AffectedStatus.UNAFFECTED)
                .build();

        Pedigree.Person son = Pedigree.Person.newBuilder()
                .setFamilyId(FAMILY_ID)
                .setIndividualId(PROBAND_ID)
                .setMaternalId(MOTHER_ID)
                .setPaternalId(FATHER_ID)
                .setSex(Sex.MALE)
                .setAffectedStatus(Pedigree.Person.AffectedStatus.AFFECTED)
                .build();

        return Pedigree.newBuilder()
                .addPersons(mother)
                .addPersons(father)
                .addPersons(son)
                .build();
    }

    @Test
    public void rareDiseaseFamily() throws Exception {

        Individual probandIndividual = Individual.newBuilder()
                .setSex(Sex.FEMALE)
                .setId(PROBAND_ID)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(Instant.parse("2018-01-01T00:00:00Z").getEpochSecond()).build())
                .build();

        PhenotypicFeature probandPhenotype = abnormalPhenotype.toBuilder()
                .setSeverity(ontologyClass("HP:0012828", "Severe"))
                .setClassOfOnset(ontologyClass("HP:0003577", "Congenital onset"))
                .build();

        Phenopacket proband = Phenopacket.newBuilder()
                .setSubject(probandIndividual)
                .addPhenotypicFeatures(probandPhenotype)
                .build();

        Individual motherIndividual = Individual.newBuilder()
                .setSex(Sex.FEMALE)
                .setId(MOTHER_ID)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(Instant.parse("1977-05-25T00:00:00Z").getEpochSecond()).build())
                .build();

        Phenopacket mother = Phenopacket.newBuilder()
                .setSubject(motherIndividual)
                .addPhenotypicFeatures(abnormalPhenotype.toBuilder()
                        .addModifiers(ontologyClass("HP:0012826", "Moderate"))
                        .build())
                .build();

        Individual fatherIndividual = Individual.newBuilder()
                .setSex(Sex.MALE)
                .setId(FATHER_ID)
                .build();

        Phenopacket father = Phenopacket.newBuilder()
                .setSubject(fatherIndividual)
                .build();

        Pedigree trio = createPedigree();

        HtsFile htsFile = HtsFile.newBuilder()
                .setFile(File.newBuilder().setPath("/path/to/vcf.gz").build())
                .setGenomeAssembly("GRCh37")
                .build();

        Family rareDiseaseFamily = Family.newBuilder()
                .setId("STUDY_ID:0000123")
                .setPedigree(trio)
                .setProband(proband)
                .addRelatives(mother)
                .addRelatives(father)
                .addHtsFiles(htsFile)
                .build();

        System.out.println(JsonFormat.printer().print(rareDiseaseFamily));
        assertThat(rareDiseaseFamily.getId(), equalTo("STUDY_ID:0000123"));
        assertThat(rareDiseaseFamily.getProband(), equalTo(proband));
        assertThat(rareDiseaseFamily.getRelativesList(), equalTo(ImmutableList.of(mother, father)));
        assertThat(rareDiseaseFamily.getPedigree(), equalTo(trio));
        assertThat(rareDiseaseFamily.getHtsFiles(0), equalTo(htsFile));
    }

    @Test
    public void toFhirBundle() throws Exception {
        PhenotypicFeature probandPhenotype = abnormalPhenotype.toBuilder()
                .setSeverity(ontologyClass("HP:0012828", "Severe"))
                .setClassOfOnset(ontologyClass("HP:0003577", "Congenital onset"))
                .build();

        Instant probandBirthInstant = Instant.parse("2018-01-01T00:00:00Z");
        Individual proband = Individual.newBuilder()
                .setSex(Sex.MALE)
                .setId(PROBAND_ID)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(probandBirthInstant.getEpochSecond()).build())
                .build();

        Phenopacket probandPacket = Phenopacket.newBuilder()
                .setSubject(proband)
                .addPhenotypicFeatures(probandPhenotype)
                .build();

        //FHIR
        Patient probandPatient = PhenoPacketConverter.createPatient(proband);
        assertThat(probandPatient.getBirthDate(), equalTo(Date.from(probandBirthInstant)));
        assertThat(probandPatient.getId(), equalTo(PROBAND_ID));
        assertThat(probandPatient.getGender(), equalTo(Enumerations.AdministrativeGender.MALE));

        //eurgh! This fails - not the same as what went in...
        //assertThat(probandSex.getSubject().equalsDeep(probandPatient), is(true));

        Condition probandCondition = createPatientCondition(probandPhenotype, probandPatient);
        assertThat(probandCondition.getCode().getCodingFirstRep().getCode(), equalTo(probandPhenotype.getType().getId()));
        assertThat(probandCondition.getCode().getCodingFirstRep().getDisplay(), equalTo(probandPhenotype.getType().getLabel()));

        PhenotypicFeature motherPhenotype = abnormalPhenotype.toBuilder()
                .setSeverity(ontologyClass("HP:0012826", "Moderate"))
                .build();

        Instant motherBirthInstant = Instant.parse("1977-05-25T00:00:00Z");
        Individual mother = Individual.newBuilder()
                .setSex(Sex.FEMALE)
                .setId(MOTHER_ID)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(motherBirthInstant.getEpochSecond()).build())
                .build();

        Phenopacket motherPacket = Phenopacket.newBuilder()
                .setSubject(mother)
                .addPhenotypicFeatures(motherPhenotype)
                .build();

        Patient motherPatient = PhenoPacketConverter.createPatient(mother);
        Condition motherCondition = createPatientCondition(motherPhenotype, motherPatient);

        Pedigree trio = createPedigree();

        //translate to Pedigree to FHIR
        FamilyMemberHistory familyMemberHistory = new FamilyMemberHistory();
        familyMemberHistory.setStatus(FamilyMemberHistory.FamilyHistoryStatus.COMPLETED);
        familyMemberHistory.setPatient(new Reference(probandPatient));
        familyMemberHistory.setRelationship(codeableConcept("http://hl7.org/fhir/ValueSet/v3-FamilyMember", "NMTH", null));//"NFTH" = Natural FaTHer
        Extension extension = new Extension();
        extension.setUrl("http://hl7.org/fhir/StructureDefinition/familymemberhistory-patient-record");
        extension.setValue(new Reference(motherPatient));
        familyMemberHistory.setExtension(Collections.singletonList(extension));

        File vcf = File.newBuilder().setPath("/path/to/vcf.gz").build();
        Family rareDiseaseSampleData = Family.newBuilder()
                .setId("STUDY_ID:0000123")
                .setPedigree(trio)
                .setProband(probandPacket)
                .addRelatives(motherPacket)
                .addHtsFiles(HtsFile.newBuilder().setGenomeAssembly("GRCh37").setFile(vcf).build())
                .build();

        System.out.println(JsonFormat.printer().print(rareDiseaseSampleData));

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setId("STUDY_ID:0000123");
        bundle.addEntry().setResource(probandPatient);
        bundle.addEntry().setResource(probandCondition);

        bundle.addEntry().setResource(motherPatient);
        bundle.addEntry().setResource(motherCondition);

        bundle.addEntry().setResource(familyMemberHistory);

        //add the mother and pedigree
        String bundleJson = FhirContext.forR4().newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        System.out.println(bundleJson);

        for (Bundle.BundleEntryComponent bundleEntryComponent : bundle.getEntry()) {
            System.out.println(bundleEntryComponent.getResource().getResourceType());
        }
    }

    /**
     * Simple utility method for converting a Phenopacket Phenotype to a FHIR Condition. Only example code, NOT production ready!
     * @param phenotype
     * @param patient
     * @return
     */
    private static Condition createPatientCondition(PhenotypicFeature phenotype, Patient patient) {
        Condition condition = new Condition().setCode(hpoConcept(phenotype.getType().getId(), phenotype.getType().getLabel()));
        condition.setSeverity(hpoConcept(phenotype.getSeverity().getId(), phenotype.getSeverity().getLabel()));
        // Fhir has oneof datetime, Age, Period, String - For this example we're going to use a string
        condition.setOnset(new StringType(phenotype.getClassOfOnset().getLabel()));
        condition.setSubject(new Reference(patient));

        if (phenotype.getNegated()){
            condition.setVerificationStatus(codeableConcept("http://terminology.hl7.org/CodeSystem/condition-ver-status", "refuted", "refuted"));
        }
        return condition;
    }
}
