package io.durbs.rtree.places.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.Immutable

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
class PaginatedPlaces<T> {

    Integer totalPlaces
    List<T> places
}
