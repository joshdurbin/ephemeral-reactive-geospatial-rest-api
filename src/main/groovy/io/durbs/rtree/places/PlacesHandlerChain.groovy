package io.durbs.rtree.places

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.rtree.places.domain.IdAssignedPlace
import io.durbs.rtree.places.domain.Place
import io.durbs.rtree.places.domain.PlaceWithDistance
import io.durbs.rtree.places.error.PlaceSubmissionValidationException
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.Jackson
import rx.functions.Func1

import javax.validation.ConstraintViolation
import javax.validation.Validator

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class PlacesHandlerChain extends GroovyChainAction {

    @Inject
    PlacesConfig placesConfig

    @Inject
    PlacesService placesService

    @Inject
    Validator validator

    @Override
    void execute() throws Exception {

        path() {

            byMethod {

                get {

                    placesService
                            .getAllPlaces()
                            .toList()
                            .subscribe { List<IdAssignedPlace> places ->

                        render Jackson.json(places)
                    }
                }

                post {

                    parse(fromJson(Place))
                            .observe()
                            .flatMap ({ Place place ->

                        Set<ConstraintViolation<Place>> violations = validator.validate(place)

                        if (!violations.empty) {
                            throw new PlaceSubmissionValidationException(violations)
                        } else {
                            placesService.savePlace(place)
                        }

                    } as Func1)
                            .single()
                            .subscribe { String placeUUID ->

                        redirect("${Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH}/${placeUUID}")
                    }
                }
            }
        }

        delete('deleteAllPlaces') {

            placesService.removeAllPlaces()
            redirect(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)
        }

        get('count') {

            render "${placesService.getNumberOfStoredPlaces()}"
        }

        get('random') {

            placesService
                    .getRandomPlace()
                    .single()
                    .subscribe { IdAssignedPlace place ->

                render Jackson.json(place)
            }
        }

        get("near/:$Constants.TOKEN_LATITUDE/:$Constants.TOKEN_LONGITUDE") {

            final Double latitude = pathTokens[Constants.TOKEN_LATITUDE] as Double
            final Double longitude = pathTokens[Constants.TOKEN_LONGITUDE] as Double

            placesService
                    .findPlacesNear(latitude, longitude, placesConfig.defaultSearchRadius)
                    .toList()
                    .subscribe { List<PlaceWithDistance> places ->

                render Jackson.json(places)
            }
        }

        get("near/:$Constants.TOKEN_LATITUDE/:$Constants.TOKEN_LONGITUDE/:$Constants.TOKEN_RADIUS") {

            final Double latitude = pathTokens[Constants.TOKEN_LATITUDE] as Double
            final Double longitude = pathTokens[Constants.TOKEN_LONGITUDE] as Double
            final Double radius = pathTokens[Constants.TOKEN_RADIUS] as Double

            final Double queryRadius

            if (radius && radius <= placesConfig.maxAllowableSearchRadius) {
                queryRadius = radius
            } else {
                queryRadius = placesConfig.defaultSearchRadius
            }

            placesService
                    .findPlacesNear(latitude, longitude, queryRadius)
                    .toSortedList( { PlaceWithDistance firstPlace, PlaceWithDistance secondPlace ->

                (firstPlace.distance - secondPlace.distance).intValue()
            })
                    .subscribe { List<PlaceWithDistance> places ->

                render Jackson.json(places)
            }
        }

        path(":${Constants.TOKEN_PLACE_ID}") {

            byMethod {

                get {

                    placesService
                            .getPlace(pathTokens[Constants.TOKEN_PLACE_ID])
                            .single()
                            .subscribe { IdAssignedPlace place ->

                        render Jackson.json(place)
                    }
                }

                delete {

                    placesService.removePlace(pathTokens[Constants.TOKEN_PLACE_ID])
                    redirect(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)
                }
            }
        }
    }
}
