package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.dstu3.model.*;
import org.phenopackets.schema.v1.PhenoPacket;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.fhir.interop.converters.fhir.IndividualExtractor;
import org.phenopackets.schema.v1.fhir.interop.converters.fhir.MetaDataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Converter for converting a FHIR bundle to a Phenopacket.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class FhirConverter {

    private static final Logger logger = LoggerFactory.getLogger(FhirConverter.class);

    // Requires a CURIE map?  https://github.com/monarch-initiative/dipper/blob/master/dipper/curie_map.yaml
    // or OntologyClassConverter?
    public FhirConverter() {
    }

    public PhenoPacket toPhenoPacket(Bundle bundle) {

        MetaDataExtractor metaDataExtractor = new MetaDataExtractor("FHIR converter");

        Map<ResourceType, List<Resource>> resourcesByType = bundle.getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .collect(groupingBy(Resource::getResourceType));

        List<Patient> patients = getPatientsFromResources(resourcesByType);
        //extract patient conditions
        List<Condition> patientConditions = getPatientConditionsFromResources(resourcesByType);

        //convert patient condition coding to metadata
        metaDataExtractor.extractPatientMetaData(patients);
        metaDataExtractor.extractConditionMetaData(patientConditions);

        IndividualExtractor individualExtractor = new IndividualExtractor();
        List<Individual> individuals = individualExtractor.extractIndividuals(patients, patientConditions);

        PhenoPacket.Builder phenoPacketBuilder = PhenoPacket.newBuilder();

        if (individuals.size() == 1) {
            phenoPacketBuilder.setPatient(individuals.get(0));
        } else if (individuals.size() > 1) {
            logger.warn("Found {} patients in this bundle - not sure who the main subject is so creating individuals", individuals.size());
            phenoPacketBuilder.addAllIndividuals(individuals);
        }

        if (metaDataExtractor.hasResources()) {
            MetaData metaData = metaDataExtractor.buildMetaData();
            // perhaps it would be better to pass in the PhenoPacketBuilder? Would also work for the Individuals
            phenoPacketBuilder.setMetaData(metaData);
        }

        return phenoPacketBuilder.build();
    }

    private List<Patient> getPatientsFromResources(Map<ResourceType, List<Resource>> resourcesByType) {
        return resourcesByType
                .getOrDefault(ResourceType.Patient, Collections.emptyList())
                .stream()
                .map(resource -> (Patient) resource)
                .collect(Collectors.toList());
    }

    private List<Condition> getPatientConditionsFromResources(Map<ResourceType, List<Resource>> resourcesByType) {
        return resourcesByType
                .getOrDefault(ResourceType.Condition, Collections.emptyList())
                .stream()
                .map(resource -> (Condition) resource)
                .filter(condition -> condition.getSubject().getResource() instanceof Patient)
                .collect(Collectors.toList());
    }
}
