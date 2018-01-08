package io.durbs.rtree.places.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.Immutable

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
class Place {

    @NotEmpty
    String name

    String address
    String city
    String state
    String zipCode
    String telephoneNumber

    @NotNull
    Double latitude

    @NotNull
    Double longitude
}
