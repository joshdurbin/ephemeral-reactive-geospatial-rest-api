package io.durbs.rtree.places

import groovy.transform.CompileStatic
import jdk.nashorn.internal.ir.annotations.Immutable

@CompileStatic
@Immutable
class PlacesConfig {

    Boolean star
    Integer minChildren
    Integer maxChildren

    Integer maxResultsPerPage

    Double defaultSearchRadius
    Double maxAllowableSearchRadius

    Integer findNearCoordinatePairObservableCacheDefaultSize
    Integer findAllObservableCacheDefaultSize
}
