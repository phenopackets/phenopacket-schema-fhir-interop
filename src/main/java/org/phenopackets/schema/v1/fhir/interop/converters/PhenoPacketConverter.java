package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.dstu3.model.*;
import org.phenopackets.schema.v1.PhenoPacket;
import org.phenopackets.schema.v1.core.Biosample;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Phenotype;

import java.time.Instant;
import java.util.*;

import static org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil.codeableConcept;

/**
 * Adaptor class for converting a FHIR message to a Phenopacket
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class PhenoPacketConverter {

    //needs to be a CurieUtil
    static final String SNOMED_CT_SYSTEM = "http://snomed.info/sct";
    static final String HPO_SYSTEM = "http://purl.obolibrary.org/obo/hp.owl";

    private PhenoPacketConverter() {
    }

    public static Bundle toFhirBundle(PhenoPacket phenoPacket) {
        if (PhenoPacket.getDefaultInstance().equals(phenoPacket)) {
            return new Bundle();
        }
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        List<Resource> resources = extractResourcesFromPhenoPacket(phenoPacket);
        resources.forEach(bundle.addEntry()::setResource);

        return bundle;
    }

    private static List<Resource> extractResourcesFromPhenoPacket(PhenoPacket phenoPacket) {
        List<Resource> resources = new ArrayList<>();
        if (phenoPacket.hasPatient()) {
            Individual individual = phenoPacket.getPatient();
            resources.addAll(createResourcesForIndividual(individual));
        }
        for (Individual individual : phenoPacket.getIndividualsList()) {
            resources.addAll(createResourcesForIndividual(individual));
        }

        if (phenoPacket.hasPedigree()){
            // make a pedigree
        }
        for (Biosample biosample : phenoPacket.getBiosamplesList()) {
            resources.add(createSpecimen(biosample));
        }

        return resources;
    }

    private static List<Resource> createResourcesForIndividual(Individual individual) {
        List<Resource> resources = new ArrayList<>();
        Patient patient = createPatient(individual);
        resources.add(patient);
        for (Phenotype phenotype : individual.getPhenotypesList()) {
            resources.add(createPatientCondition(phenotype, patient));
        }
        return resources;
    }

    private static Resource createSpecimen(Biosample biosample) {
        Specimen specimen = new Specimen();
        specimen.setId(biosample.getId());
        biosample.getTaxonomy();
        biosample.getPhenotypesList();// What to do with these? Specimen only has one type...
        OntologyClass sampleType = biosample.getType();
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
        //TODO: make generic for handling timestamp or Age wth "P1Y3M" etc.
        //TODO: remember! all fields are optional
        patient.setBirthDate(Date.from(Instant.ofEpochSecond(individual.getDateOfBirth().getSeconds())));
        Enumerations.AdministrativeGender administrativeGender = convertPatoToAdministrativeGender(individual.getSex());
        patient.setGender(administrativeGender);
        return patient;
    }

    /**
     * Simple utility method for converting a Phenopacket Phenotype to a FHIR Condition. Only example code, NOT production ready!
     * @param phenotype
     * @param patient
     * @return
     */
    public static Condition createPatientCondition(Phenotype phenotype, Patient patient) {
        Condition condition = new Condition();
        //TODO: Use CurieUtil to convert the CURIE to a full system
        condition.setCode(codeableConcept(HPO_SYSTEM, phenotype.getType().getId(), phenotype.getType().getLabel()));
        condition.setSeverity(codeableConcept(HPO_SYSTEM, phenotype.getSeverity().getId(), phenotype.getSeverity().getLabel()));
        // Fhir has oneof datetime, Age, Period, String - For this example we're going to use a string
        condition.setOnset(new StringType(phenotype.getClassOfOnset().getLabel()));
        condition.setSubject(new Reference(patient));

        if (phenotype.getNegated()){
            condition.setVerificationStatus(Condition.ConditionVerificationStatus.REFUTED);
        }
        return condition;
    }

    public static Observation createSexObservation(Individual individual, Patient patient) {
        OntologyClass sexClasss = individual.getSex();
        //TODO: needs to do a look-up against the metadata object to get the system for this. Right now this is hard-coded to use pato.owl.
        CodeableConcept sexConcept = codeableConcept("http://purl.obolibrary.org/obo/pato.owl", sexClasss.getId(), sexClasss.getLabel());
        Observation observation = new Observation();
        observation.setCode(sexConcept);
        observation.setSubject(new Reference(patient));
        return observation;
    }

    // This is horribly brittle - might be safest to bin this and just use the observation or directly model sex as an Enum
    private static Enumerations.AdministrativeGender convertPatoToAdministrativeGender(OntologyClass sex) {
        if (!sex.getId().startsWith("PATO") || sex.getId().isEmpty()) {
            return Enumerations.AdministrativeGender.NULL;
        }
        switch (sex.getId()) {
            case "PATO_0000383":
            case "PATO:0000383":
                return Enumerations.AdministrativeGender.FEMALE;
            case "PATO_0000384":
            case "PATO:0000384":
                return Enumerations.AdministrativeGender.MALE;
            default:
                return Enumerations.AdministrativeGender.UNKNOWN;
        }
    }

}
