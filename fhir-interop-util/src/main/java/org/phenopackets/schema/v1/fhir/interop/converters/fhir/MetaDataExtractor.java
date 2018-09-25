package org.phenopackets.schema.v1.fhir.interop.converters.fhir;

import com.google.protobuf.Timestamp;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Patient;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.core.Resource;
import org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class MetaDataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(MetaDataExtractor.class);

    private static final Resource PATO_RESOURCE = Resource.newBuilder()
            .setNamespacePrefix("PATO")
            .setId("pato")
            .setUrl("http://purl.obolibrary.org/obo/pato.owl")
            .setVersion("2018-08-14")
            .setName("Phenotype And Trait Ontology")
            .build();

    private final Set<Resource> metaDataResources;
    private String createdBy;

    public MetaDataExtractor(String createdBy) {
        metaDataResources = new LinkedHashSet<>();
        this.createdBy = createdBy;
    }

    public boolean hasResources() {
        return !metaDataResources.isEmpty();
    }
    public void extractConditionMetaData(Collection<Condition> conditions) {
        List<Coding> codings = new ArrayList<>();

        for (Condition condition : conditions) {
            if (condition.hasCode()) {
                Coding coding = condition.getCode().getCodingFirstRep();
                codings.add(coding);
            }
            if (condition.hasSeverity()) {
                Coding coding = condition.getSeverity().getCodingFirstRep();
                codings.add(coding);
            }
//            if (condition.hasEvidence()) {
//                Coding coding = condition.getEvidence().getCodingFirstRep();
//                codings.add(coding);
//            }
        }

        // TODO: Create a ConditionCodingAccumulator (should be others for other types)?
        codings.stream()
                .map(ConverterUtil::makePhenopacketResource)
                .forEach(metaDataResources::add);


    }

    public void extractPatientMetaData(Collection<Patient> patients) {
        for (Patient patient : patients) {
            if (patient.hasGender()) {
                switch (patient.getGender()) {
                    case MALE:
                    case FEMALE:
                        metaDataResources.add(PATO_RESOURCE);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public MetaData buildMetaData() {
        if (metaDataResources.isEmpty()) {
            return MetaData.getDefaultInstance();
        }
        return MetaData.newBuilder()
                .addAllResources(metaDataResources)
                .setCreated(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                .setCreatedBy(createdBy)
                .build();
    }
}
