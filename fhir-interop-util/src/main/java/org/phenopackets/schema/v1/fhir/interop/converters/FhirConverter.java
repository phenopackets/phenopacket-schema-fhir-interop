package org.phenopackets.schema.v1.fhir.interop.converters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.protobuf.Timestamp;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.*;
import org.phenopackets.schema.v1.PhenoPacket;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.core.Phenotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converter for converting a FHIR bundle to a Phenopacket.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class FhirConverter {

    private static final Logger logger = LoggerFactory.getLogger(FhirConverter.class);

    private final Map<String, Individual.Builder> individualBuildersById = new LinkedHashMap<>();
    private final ListMultimap<String, Phenotype> phenotypesByIndividualId = LinkedListMultimap.create();
    private final Set<org.phenopackets.schema.v1.core.Resource> metaDataResources = new LinkedHashSet<>();

    // Requires a CURIE map?  https://github.com/monarch-initiative/dipper/blob/master/dipper/curie_map.yaml
    // or OntologyClassConverter?
    public FhirConverter() {
    }

    public PhenoPacket toPhenoPacket(Bundle bundle) {
        PhenoPacket.Builder phenoPacketBuilder = PhenoPacket.newBuilder();

        //TODO: iterate and map/collect each type then create the phenopacket without having to use class fields?

        List<Resource> resources = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .collect(Collectors.toList());

        List<Individual.Builder> individualBuilders = resources.stream()
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> buildPatient((Patient) resource))
                .collect(Collectors.toList());


        for (Bundle.BundleEntryComponent bundleEntryComponent: bundle.getEntry()) {
            Resource resource = bundleEntryComponent.getResource();
            switch(resource.getResourceType()) {
                case Patient:
                    handlePatient((Patient) resource);
                    break;
                case Condition:
                    handleCondition((Condition) resource);
                    break;
            }
        }

        if (individualBuildersById.size() > 1) {
            logger.warn("Found {} patients in this bundle - not sure who the main subject is", individualBuildersById.size());
        }
        for (Individual.Builder individualBuilder : individualBuildersById.values()) {
            String id = individualBuilder.getId();
            if (phenotypesByIndividualId.containsKey(id)) {
                List<Phenotype> phenotypes = phenotypesByIndividualId.get(id);
                logger.debug("Adding patient phenotypes {} {}", id, phenotypes);
                individualBuilder.addAllPhenotypes(phenotypes);
            }
            phenoPacketBuilder.setPatient(individualBuilder.build());
        }

        MetaData metaData = MetaData.newBuilder()
                .addAllResources(metaDataResources)
                .setCreated(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                .setCreatedBy("FHIR converter")
                .build();
        phenoPacketBuilder.setMetaData(metaData);

        return phenoPacketBuilder.build();
    }

    private void handlePatient(Patient patient) {
        Individual.Builder individualBuilder = buildPatient(patient);

        if (individualBuildersById.containsKey(individualBuilder.getId())) {
            Individual.Builder preExisting = individualBuildersById.get(individualBuilder.getId());
            preExisting.mergeFrom(individualBuilder.buildPartial());
        } else {
            individualBuildersById.put(individualBuilder.getId(), individualBuilder);
        }
    }

    private Individual.Builder buildPatient(Patient patient) {
        logger.debug("{} {}", patient.getResourceType(), patient.getId());
        Individual.Builder individualBuilder = Individual.newBuilder();
        individualBuilder.setId(patient.getId());
        if (patient.hasBirthDate()){
            individualBuilder.setDateOfBirth(Timestamp.newBuilder().setSeconds(Instant.ofEpochMilli(patient.getBirthDate().getTime()).getEpochSecond()).build());
        }
        if (patient.hasGender()) {
            metaDataResources.add(org.phenopackets.schema.v1.core.Resource.newBuilder()
                    .setNamespacePrefix("PATO")
                    .setId("pato")
                    .setUrl("http://purl.obolibrary.org/obo/pato.owl")
                    .setVersion("2018-08-14")
                    .setName("")
                    .build());
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
        return individualBuilder;
    }

    private void handleCondition(Condition condition) {
        Coding coding = condition.getCode().getCodingFirstRep();
        IBaseResource subjectResource = condition.getSubject().getResource();
        // TODO should we be using IdElement? i.e. subjectResource.getIdElement());
        if (subjectResource instanceof Patient) {
            Patient patient = (Patient) subjectResource;
            logger.debug("{} {} {} {} {}", condition.getResourceType(), patient.getId(), coding.getSystem(), coding.getCode(), coding.getDisplay());

            org.phenopackets.schema.v1.core.Resource resource = ConverterUtil.makePhenopacketResource(coding);
            metaDataResources.add(resource);

            Phenotype phenotype = ConverterUtil.makePhenotype(condition);
            phenotypesByIndividualId.put(patient.getId(), phenotype);
        }

    }

}
