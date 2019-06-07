package org.phenopackets.schema.v1.fhir.interop.converters.fhir;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.core.Resource;
import org.phenopackets.schema.v1.fhir.interop.converters.ConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class MetaDataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(MetaDataExtractor.class);

    private MetaDataExtractor() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String createdBy = "Unspecified";
        private List<Condition> conditions = ImmutableList.of();

        private Builder() {
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder fromConditions(Iterable<Condition> conditions) {
            this.conditions = ImmutableList.copyOf(conditions);
            return this;
        }

        public MetaData buildMetaData() {
            Set<Resource> metaDataResources = populateResources();

            return MetaData.newBuilder()
                    .addAllResources(metaDataResources)
                    .setCreated(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                    .setCreatedBy(createdBy)
                    .build();
        }

        private Set<Resource> populateResources() {
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
            return codings.stream()
                    .map(ConverterUtil::makePhenopacketResource)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
