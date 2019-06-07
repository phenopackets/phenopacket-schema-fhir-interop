package org.phenopackets.schema.v1.fhir.interop.converters.fhir;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.protobuf.Timestamp;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.core.PhenotypicFeature;
import org.phenopackets.schema.v1.core.Sex;
import org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
    // might need an extractIndividualForId(Individual.id)
public class PhenopacketExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PhenopacketExtractor.class);

    public Phenopacket extractPhenopacket(Patient patient, List<Condition> patientConditions) {
        //do we need Observations?
        //convert conditions to patient phenotypes
        Map<String, Collection<PhenotypicFeature>> patientPhenotypes = extractPatientPhenotypes(patientConditions);
        logger.debug("patientPhenotypes: {}", patientPhenotypes);

        //convert patient condition coding to metadata
        MetaData metaData = MetaDataExtractor.builder()
                .createdBy("FHIR converter")
                .fromConditions(patientConditions)
                // TODO: sort out Condition -> Resource or PhenotypicFeature ->  Resource
                .buildMetaData();

        return buildPhenopacket(patient, patientPhenotypes, metaData);
    }

    private Map<String, Collection<PhenotypicFeature>> extractPatientPhenotypes(Collection<Condition> patientConditions){
        ListMultimap<String, PhenotypicFeature> phenotypes = LinkedListMultimap.create();

        for (Condition condition : patientConditions) {
            IBaseResource subjectResource = condition.getSubject().getResource();
            //should't need to do this, but just in case...
            if (subjectResource instanceof Patient) {
                Patient patient = (Patient) subjectResource;
                PhenotypicFeature phenotype = ConverterUtil.makePhenotypicFeature(condition);
                phenotypes.put(patient.getId(), phenotype);
            }
        }

        return phenotypes.asMap();
    }

    private Phenopacket buildPhenopacket(Patient patient, Map<String, Collection<PhenotypicFeature>> patientPhenotypes, MetaData metaData) {
        Phenopacket.Builder phenopacketBuilder = Phenopacket.newBuilder();

        Individual subject = buildIndividual(patient);
        phenopacketBuilder.setSubject(subject);

        String patientId = patient.getId();
        if (patientPhenotypes.containsKey(patientId)) {
            Collection<PhenotypicFeature> phenotypicFeatures = patientPhenotypes.get(patientId);
            logger.debug("Adding patient phenotypes {} {}", patientId, phenotypicFeatures);
            phenopacketBuilder.addAllPhenotypicFeatures(phenotypicFeatures);
        }

        phenopacketBuilder.setMetaData(metaData);

        return phenopacketBuilder.build();
    }

    private Individual buildIndividual(Patient patient) {
        String patientId = patient.getId();
        logger.debug("{} {}", patient.getResourceType(), patientId);
        Individual.Builder individualBuilder = Individual.newBuilder();
        individualBuilder.setId(patientId);
        if (patient.hasBirthDate()){
            Instant patientBirthInstant = patient.getBirthDate().toInstant();
            Timestamp dateOfBirth = Timestamp.newBuilder()
                    .setSeconds(patientBirthInstant.getEpochSecond())
                    .build();
            individualBuilder.setDateOfBirth(dateOfBirth);
        }
        if (patient.hasGender()) {
            Sex individualSex = toSex(patient.getGender());
            individualBuilder.setSex(individualSex);
        }
        return individualBuilder.build();
    }

    private Sex toSex(Enumerations.AdministrativeGender administrativeGender) {
        switch (administrativeGender) {
            case MALE:
                return Sex.MALE;
            case FEMALE:
                return Sex.FEMALE;
            case OTHER:
                return Sex.OTHER_SEX;
            default:
                return Sex.UNKNOWN_SEX;
        }
    }
}
