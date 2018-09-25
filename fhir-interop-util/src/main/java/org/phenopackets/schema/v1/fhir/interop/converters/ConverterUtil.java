package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Phenotype;
import org.phenopackets.schema.v1.core.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super-simple utility class for saving a lot of verbose typing for common operations.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class ConverterUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConverterUtil.class);

    private ConverterUtil() {

    }

    public static CodeableConcept codeableConcept(String system, String id, String label){
        return new CodeableConcept().addCoding(new Coding(system, id, label));
    }

    public static OntologyClass ontologyClass(String id, String label) {
        return OntologyClass.newBuilder()
                .setId(id)
                .setLabel(label)
                .build();
    }

    public static Phenotype makePhenotype(Condition condition) {
        if (!condition.hasCode()) {
            logger.warn("Condition has no code - skipping");
            return Phenotype.getDefaultInstance();
        }
        Coding coding = condition.getCode().getCodingFirstRep();
        Phenotype.Builder phenotypeBuilder = Phenotype.newBuilder();
        phenotypeBuilder.setType(ConverterUtil.ontologyClass(coding.getCode(), coding.getDisplay()));
        if (condition.hasSeverity()) {
            CodeableConcept severity = condition.getSeverity();
            phenotypeBuilder.setSeverity(ConverterUtil.ontologyClass(severity.getCodingFirstRep().getCode(), severity.getCodingFirstRep().getDisplay()));
        }
        if (condition.hasOnsetStringType()){
            try {
                phenotypeBuilder.setClassOfOnset(ConverterUtil.ontologyClass("", condition.getOnsetStringType().getValue()));
            } catch (FHIRException e) {
                logger.error("Unable to get Condition::onsetStringType {}", e);
            }
        }
        if (condition.hasEvidence()) {
            //TODO: handle evidence
        }
        return phenotypeBuilder.build();
    }

    public static Resource makePhenopacketResource(Coding coding) {
        // just give it a punt - ':' and '_' are common identifier separators
        String[] tokens = coding.getCode().split("[:_]");
        String prefix = (tokens.length == 0) ? "" : tokens[0];

        return org.phenopackets.schema.v1.core.Resource.newBuilder()
                .setNamespacePrefix(prefix)
                .setId(prefix.toLowerCase())
                .setUrl(coding.getSystem())
                .setVersion(coding.getVersion() == null ? "" : coding.getVersion())
                .build();
    }
}
