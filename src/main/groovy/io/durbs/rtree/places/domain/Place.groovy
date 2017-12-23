package io.durbs.rtree.places.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.Immutable

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
class Place {

    String name
    String address
    String city
    String state
    String zipCode
    String telephoneNumber
    Double latitude
    Double longitude
}
