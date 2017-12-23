package io.durbs.rtree.places.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.Immutable

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
class PlaceWithDistance {

    Double distance

    @Delegate
    @JsonIgnore
    IdAssignedPlace place
}
