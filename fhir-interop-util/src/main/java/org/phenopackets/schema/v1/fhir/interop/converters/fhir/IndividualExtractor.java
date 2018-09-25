package org.phenopackets.schema.v1.fhir.interop.converters.fhir;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.protobuf.Timestamp;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.Phenotype;
import org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class IndividualExtractor {

    private static final Logger logger = LoggerFactory.getLogger(IndividualExtractor.class);

    public List<Individual> extractIndividuals(List<Patient> patients, List<Condition> patientConditions) {
        //do we need Observations?
        //convert conditions to patient phenotypes
        Map<String, Collection<Phenotype>> patientPhenotypes = extractPatientPhenotypes(patientConditions);
        logger.debug("patientPhenotypes: {}", patientPhenotypes);

        return patients.stream()
                .map(patient -> this.buildIndividual(patient, patientPhenotypes))
                .collect(toList());
    }

    private Map<String, Collection<Phenotype>> extractPatientPhenotypes(Collection<Condition> patientConditions){
        ListMultimap<String, Phenotype> phenotypes = LinkedListMultimap.create();

        for (Condition condition : patientConditions) {
            IBaseResource subjectResource = condition.getSubject().getResource();
            //should't need to do this, but just in case...
            if (subjectResource instanceof Patient) {
                Patient patient = (Patient) subjectResource;
                Phenotype phenotype = ConverterUtil.makePhenotype(condition);
                phenotypes.put(patient.getId(), phenotype);
            }
        }

        return phenotypes.asMap();
    }

    private Individual buildIndividual(Patient patient, Map<String, Collection<Phenotype>> patientPhenotypes) {
        String patientId = patient.getId();
        logger.debug("{} {}", patient.getResourceType(), patientId);
        Individual.Builder individualBuilder = Individual.newBuilder();
        individualBuilder.setId(patientId);
        if (patient.hasBirthDate()){
            Timestamp dateOfBirth = Timestamp.newBuilder()
                    .setSeconds(patient.getBirthDate().toInstant().getEpochSecond())
                    .build();
            individualBuilder.setDateOfBirth(dateOfBirth);
        }
        if (patient.hasGender()) {
            switch (patient.getGender()) {
                case MALE:
                    individualBuilder.setSex(ConverterUtil.ontologyClass("PATO:0000384", "male"));
                    break;
                case FEMALE:
                    individualBuilder.setSex(ConverterUtil.ontologyClass("PATO:0000383", "female"));
                    break;
                default:
                    break;
            }
        }
        if (patientPhenotypes.containsKey(patientId)) {
            Collection<Phenotype> phenotypes = patientPhenotypes.get(patientId);
            logger.debug("Adding patient phenotypes {} {}", patientId, phenotypes);
            individualBuilder.addAllPhenotypes(phenotypes);
        }
        return individualBuilder.build();
    }
}
