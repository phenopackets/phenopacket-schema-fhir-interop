package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.r4.model.*;
import org.phenopackets.schema.v1.Cohort;
import org.phenopackets.schema.v1.Family;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.fhir.interop.converters.fhir.PhenopacketExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

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

    public Phenopacket toPhenopacket(Bundle bundle) {

        Map<ResourceType, List<Resource>> resourcesByType = bundle.getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .collect(groupingBy(Resource::getResourceType));

        List<Patient> patients = getPatientsFromResources(resourcesByType);
        //extract patient conditions
        List<Condition> patientConditions = getPatientConditionsFromResources(resourcesByType);

        if (patients.size() > 1) {
            logger.warn("Found {} patients in this bundle - not sure who the main subject is so returning first", patients
                    .size());
        }

        PhenopacketExtractor phenopacketExtractor = new PhenopacketExtractor();

        return patients.stream()
                .map(patient -> phenopacketExtractor.extractPhenopacket(patient, patientConditions))
                .findFirst()
                .orElse(Phenopacket.getDefaultInstance());
    }

//    Bundle-> Phenopacket:
//    Takes first patient and creates phenopacket. Explodes/warns if more than one patient is present.
//    Bundle -> Cohort:
//    Extracts all patients, converts to phenopackets, adds to Cohort.
//            Bundle -> Family
//    Extracts patients…. Needs pedigree checking/conversion logic.

    public Cohort toCohort(Bundle bundle) {
        return null;
    }

    public Family toFamily(Bundle bundle) {
        // TODO Does FHIR have a means of indicating a proband? If not this won't work.
        // although could do it with a bundle containing a single Patient and a FamilyMemberHistoryGenetic
        // however we still don't know who tha proband is unless we use some heuristic to pick the patient with the
        // largest number of Conditions, which is pure guesswork.
        return null;
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
