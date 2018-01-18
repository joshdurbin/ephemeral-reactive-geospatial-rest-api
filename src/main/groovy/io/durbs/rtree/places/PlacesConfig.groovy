package io.durbs.rtree.places

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class PlacesConfig {

    Boolean star
    Integer minChildren
    Integer maxChildren

    Integer maxResultsPerPage

    Double defaultSearchRadius
    Double maxAllowableSearchRadius

    Integer findNearObservableCacheInitialCapacity
    Integer findAllObservableCacheInitialCapacity
}
