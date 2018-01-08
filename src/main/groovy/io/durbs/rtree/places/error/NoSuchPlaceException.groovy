package io.durbs.rtree.places.error

import com.github.gregwhitaker.ratpack.error.BaseException
import groovy.transform.CompileStatic

@CompileStatic
class NoSuchPlaceException extends BaseException {

    NoSuchPlaceException() {

        super(500, 'Places tree is empty or the requested place does not exist.')
    }
}
