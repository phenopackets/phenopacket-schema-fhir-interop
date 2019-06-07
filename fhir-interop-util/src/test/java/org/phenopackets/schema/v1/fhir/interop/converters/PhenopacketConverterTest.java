package org.phenopackets.schema.v1.fhir.interop.converters;

import ca.uhn.fhir.context.FhirContext;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil.ontologyClass;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class PhenopacketConverterTest {

    private static final String PROBAND_ID = "PROBAND:1";
    private static final String MOTHER_ID = "MOTHER:1";
    private static final String FATHER_ID = "FATHER:1";

    private static final OntologyClass FEMALE = ontologyClass("PATO:0000383", "female");
    private static final OntologyClass MALE = ontologyClass("PATO:0000384", "male");

    private static final CodeableConcept FHIR_FEMALE = ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/pato.owl", FEMALE.getId(), FEMALE.getLabel());
    private static final CodeableConcept FHIR_MALE = ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/pato.owl", MALE.getId(), MALE.getLabel());

    private Pedigree createPedigree() {
        Pedigree.Person mother = Pedigree.Person.newBuilder()
                .setIndividualId(MOTHER_ID)
                .setSex(Sex.FEMALE)
                .setAffectedStatus(Pedigree.Person.AffectedStatus.AFFECTED)
                .build();

        Pedigree.Person father = Pedigree.Person.newBuilder()
                .setIndividualId(FATHER_ID)
                .setSex(Sex.MALE)
                .setAffectedStatus(Pedigree.Person.AffectedStatus.UNAFFECTED)
                .build();

        Pedigree.Person son = Pedigree.Person.newBuilder()
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

    private static final PhenotypicFeature abnormalPhenotype = PhenotypicFeature.newBuilder()
            .setType(ontologyClass("HP:0000118", "Phenotypic abnormality"))
            .build();

    @Test
    public void emptyPhenopacketReturnsEmptyBundle() {
        Phenopacket phenoPacket = Phenopacket.getDefaultInstance();
        Bundle result = PhenoPacketConverter.toFhirBundle(phenoPacket);

        assertThat(result.isEmpty(), is(true));
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

        Phenopacket probandPhenopacket = Phenopacket.newBuilder()
                .setSubject(proband)
                .addPhenotypicFeatures(probandPhenotype)
                .build();

        //FHIR
        Patient probandPatient = PhenoPacketConverter.createPatient(proband);
        assertThat(probandPatient.getBirthDate(), equalTo(Date.from(probandBirthInstant)));
        assertThat(probandPatient.getId(), equalTo(PROBAND_ID));
        assertThat(probandPatient.getGender(), equalTo(Enumerations.AdministrativeGender.MALE));

        Condition probandCondition = PhenoPacketConverter.createPatientCondition(probandPhenotype, probandPatient);
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

        Phenopacket motherPhenopacket = Phenopacket.newBuilder()
                .setSubject(mother)
                .addPhenotypicFeatures(motherPhenotype)
                .build();


        Patient motherPatient = PhenoPacketConverter.createPatient(mother);
        Condition motherCondition = PhenoPacketConverter.createPatientCondition(motherPhenotype, motherPatient);

        Pedigree trio = createPedigree();

        //translate to Pedigree to FHIR
        FamilyMemberHistory familyMemberHistory = new FamilyMemberHistory();
        familyMemberHistory.setStatus(FamilyMemberHistory.FamilyHistoryStatus.COMPLETED);
        familyMemberHistory.setPatient(new Reference(probandPatient));
        familyMemberHistory.setRelationship(ConverterUtil.codeableConcept("http://hl7.org/fhir/ValueSet/v3-FamilyMember", "NMTH", null));//"NFTH" = Natural FaTHer
        Extension extension = new Extension();
        extension.setUrl("http://hl7.org/fhir/StructureDefinition/familymemberhistory-patient-record");
        extension.setValue(new Reference(motherPatient));
        familyMemberHistory.setExtension(Collections.singletonList(extension));

        File vcf = File.newBuilder().setPath("/path/to/vcf.gz").build();
        HtsFile vcfFile = HtsFile.newBuilder().setFile(vcf).setGenomeAssembly("GRCh37").build();

        Phenopacket rareDiseaseSampleData = Phenopacket.newBuilder()
                .setId("STUDY_ID:0000123")
//                .setPedigree(trio)
                .setSubject(proband)
//                .addIndividuals(mother)
//                .setGenomeAssembly(GenomeAssembly.GRCH_37)
//                .setVcf(vcf)
                .addHtsFiles(vcfFile)
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
}