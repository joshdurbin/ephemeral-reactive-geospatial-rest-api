package io.durbs.rtree.places.error

import com.github.gregwhitaker.ratpack.error.BaseFieldException
import groovy.transform.CompileStatic
import io.durbs.rtree.places.domain.Place

import javax.validation.ConstraintViolation

@CompileStatic
class PlaceSubmissionValidationException extends BaseFieldException {

    PlaceSubmissionValidationException(Set<ConstraintViolation<Place>> constraintViolations) {

        super(400, 'One or more required properties for Place was not defined.')

        constraintViolations.each { ConstraintViolation<Place> violation ->
            addField(violation.propertyPath.toString(), violation.message)
        }
    }
}
