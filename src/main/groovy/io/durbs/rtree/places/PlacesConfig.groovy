package io.durbs.rtree.places

import groovy.transform.CompileStatic
import jdk.nashorn.internal.ir.annotations.Immutable

@CompileStatic
@Immutable
class PlacesConfig {

    Boolean star
    Integer minChildren
    Integer maxChildren

    Integer maxResults

    Double defaultSearchRadius
    Double maxAllowableSearchRadius
}
