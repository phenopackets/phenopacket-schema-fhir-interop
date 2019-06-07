package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Resource;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;

import java.time.Instant;
import java.util.*;

import static org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil.codeableConcept;

/**
 * Converter for converting a Phenopacket to a FHIR bundle
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class PhenoPacketConverter {

    //needs to be a CurieUtil
    static final String SNOMED_CT_SYSTEM = "http://snomed.info/sct";
    static final String HPO_SYSTEM = "http://purl.obolibrary.org/obo/hp.owl";

    private PhenoPacketConverter() {
    }

    public static Bundle toFhirBundle(Phenopacket phenoPacket) {
        if (Phenopacket.getDefaultInstance().equals(phenoPacket)) {
            return new Bundle();
        }
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        List<Resource> resources = extractResourcesFromPhenoPacket(phenoPacket);
        resources.forEach(bundle.addEntry()::setResource);

        return bundle;
    }

    private static List<Resource> extractResourcesFromPhenoPacket(Phenopacket phenoPacket) {
        List<Resource> resources = new ArrayList<>();
        if (phenoPacket.hasSubject()) {
            Individual individual = phenoPacket.getSubject();
            Patient patient = createPatient(individual);
            resources.add(patient);
            for (PhenotypicFeature phenotype : phenoPacket.getPhenotypicFeaturesList()) {
                resources.add(createPatientCondition(phenotype, patient));
            }
        }

        for (Biosample biosample : phenoPacket.getBiosamplesList()) {
            resources.add(createSpecimen(biosample));
        }

        return resources;
    }

    private static Resource createSpecimen(Biosample biosample) {
        Specimen specimen = new Specimen();
        specimen.setId(biosample.getId());
        biosample.getTaxonomy();
        biosample.getPhenotypicFeaturesList();// What to do with these? Specimen only has one type...
        OntologyClass sampleType = biosample.getSampledTissue();
        // TODO:
        // look-up against MetaData object and use the CureUtil to create long and short-form identifiers
        // this can then convert any OntologyClass into a CodeableConcept.
        // reversing this should also be possible.
        specimen.setType(codeableConcept(SNOMED_CT_SYSTEM, sampleType.getId(), sampleType.getLabel()));
        specimen.setSubject(new Reference(biosample.getIndividualId()));
        return specimen;
    }

    public static Patient createPatient(Individual individual) {
        Patient patient = new Patient();
        patient.setId(individual.getId());
        if (individual.getDateOfBirth().isInitialized()) {
            patient.setBirthDate(Date.from(Instant.ofEpochSecond(individual.getDateOfBirth().getSeconds())));
        }
        patient.setGender(asAdministrativeGender(individual.getSex()));
        return patient;
    }

    /**
     * Simple utility method for converting a Phenopacket Phenotype to a FHIR Condition. Only example code, NOT production ready!
     * @param phenotype
     * @param patient
     * @return
     */
    public static Condition createPatientCondition(PhenotypicFeature phenotype, Patient patient) {
        Condition condition = new Condition();
        //TODO: Use CurieUtil to convert the CURIE to a full system
        condition.setCode(codeableConcept(HPO_SYSTEM, phenotype.getType().getId(), phenotype.getType().getLabel()));
        condition.setSeverity(codeableConcept(HPO_SYSTEM, phenotype.getSeverity().getId(), phenotype.getSeverity().getLabel()));
        // Fhir has oneof datetime, Age, Period, String - For this example we're going to use a string
        condition.setOnset(new StringType(phenotype.getClassOfOnset().getLabel()));
        condition.setSubject(new Reference(patient));

        if (phenotype.getNegated()){
            // absolutely no idea how to do this in R4
            // DSTU3 was condition.setVerificationStatus(Condition.ConditionVerificationStatus.REFUTED);
            condition.setVerificationStatus(codeableConcept("http://terminology.hl7.org/CodeSystem/condition-ver-status", "refuted", "refuted"));
        }
        return condition;
    }

    private static Enumerations.AdministrativeGender asAdministrativeGender(Sex sex) {
        switch (sex) {
            case MALE:
                return Enumerations.AdministrativeGender.MALE;
            case FEMALE:
                return Enumerations.AdministrativeGender.FEMALE;
            case OTHER_SEX:
                return Enumerations.AdministrativeGender.OTHER;
            case UNKNOWN_SEX:
                return Enumerations.AdministrativeGender.UNKNOWN;
            case UNRECOGNIZED:
            default:
                return Enumerations.AdministrativeGender.NULL;
        }
    }

}
