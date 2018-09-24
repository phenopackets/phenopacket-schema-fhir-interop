package org.phenopackets.schema.v1.fhir.interop.converters;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.phenopackets.schema.v1.core.MetaData;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Resource;
import org.prefixcommons.CurieUtil;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This is *not* suitably robust for proper production work
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class OntologyClassConverter {

    private final CurieUtil curieUtil;
    private final Map<String, String> resourcePrefixMappings;

    private OntologyClassConverter(Map<String, String> resourcePrefixMappings) {
        this.resourcePrefixMappings = resourcePrefixMappings;
        curieUtil = new CurieUtil(resourcePrefixMappings);
    }

    public static OntologyClassConverter fromMetaData(MetaData metaData) {
        Map<String, String> resourcePrefixMappings = metaData.getResourcesList()
                .stream()
                .collect(Collectors.toMap(Resource::getNamespacePrefix, Resource::getUrl, (a, b) -> b));

        return new OntologyClassConverter(resourcePrefixMappings);
    }

    /**
     * Key = URL, value = prefix. For example the HPO = Map.of("http://purl.obolibrary.org/obo/HP_", "HP")
     *
     * @param resourceCurieMappings
     * @return
     */
    public static OntologyClassConverter fromMap(Map<String, String> resourceCurieMappings) {
        // Requires a CURIE map?  https://github.com/monarch-initiative/dipper/blob/master/dipper/curie_map.yaml
        // and a system map e.g. "http://purl.obolibrary.org/obo/hp.owl" : "HP"
        // then can map a CodeableConcept or an OntologyClass to an expanded URI if they are using CURIEs as their id,
        // which is the recommendation in Phenopackets.
        return new OntologyClassConverter(resourceCurieMappings);
    }

    public CodeableConcept toCodeableConcept(OntologyClass ontologyClass) {
        String ontologyClassId = ontologyClass.getId();
        Optional<String> iri = curieUtil.getIri(ontologyClassId);
        Optional<String> curie = curieUtil.getCurie(iri.get());
        String expansion = curieUtil.getExpansion(ontologyClassId.split(":")[0]);

//        String system = resourcePrefixMappings.get();
//        if (system == null) {
//
//        }
        // TODO... need to map the prefix 'HP' to the system 'http://purl.obolibrary.org/obo/hp.owl' and the id 'HP:0001156'
        // things like LOINC and SNOMED do not use CURIEs in FHIR, but the phenopacket recommends using a CURIE
        // see http://www.hl7.org/fhiR/terminologies-systems.html
        return ConverterUtil.codeableConcept("http://purl.obolibrary.org/obo/hp.owl", ontologyClass.getId(), ontologyClass.getLabel());
    }

    public OntologyClass toOntologyClass(CodeableConcept codeableConcept) {
        Coding coding = codeableConcept.getCodingFirstRep(); //should expect many...
        String system = coding.getSystem();
        return toOntologyClass(coding);

    }

    private OntologyClass toOntologyClass(Coding coding) {
        Optional<String> id = Optional.ofNullable(coding.getCode());
        Optional<String> text = Optional.ofNullable(coding.getDisplay());

        if (id.isPresent() || text.isPresent()) {
            return ConverterUtil.ontologyClass(id.orElse(""), text.orElse(""));
        }

        return OntologyClass.getDefaultInstance();
    }
}
