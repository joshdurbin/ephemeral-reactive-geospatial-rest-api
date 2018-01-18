package io.durbs.rtree.places

import static ratpack.jackson.Jackson.fromJson

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.rtree.places.domain.IdAssignedPlace
import io.durbs.rtree.places.domain.PaginatedPlaces
import io.durbs.rtree.places.domain.Place
import io.durbs.rtree.places.error.PlaceSubmissionValidationException
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.Jackson
import rx.functions.Func1

import javax.validation.ConstraintViolation
import javax.validation.Validator

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

                    final Integer pageNumberParam

                    if (context.request.queryParams.get(Constants.QUERY_PARAM_PAGE)?.isNumber()) {
                        pageNumberParam = (context.request.queryParams.get(Constants.QUERY_PARAM_PAGE) as Integer).abs()
                    } else {
                        pageNumberParam = Constants.DEFAULT_FIRST_PAGE
                    }

                    placesService
                            .getAllPlaces(pageNumberParam)
                            .singleOrDefault(new PaginatedPlaces(totalPlaces: 0, places: []))
                            .subscribe { PaginatedPlaces paginatedPlaces ->

                        render(Jackson.json(paginatedPlaces))
                    }
                }

                post {

                    parse(fromJson(Place))
                            .observe()
                            .flatMap ({ Place place ->

                        Set<ConstraintViolation<Place>> violations = validator.validate(place)

                        if (violations.empty) {
                            placesService.savePlace(place)
                        } else {
                            throw new PlaceSubmissionValidationException(violations)
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

        get('random') {

            placesService
                    .getRandomPlace()
                    .firstOrDefault(null)
                    .subscribe { IdAssignedPlace place ->

                place ? render(Jackson.json(place)) : clientError(400)
            }
        }

        get("near/:$Constants.TOKEN_LATITUDE/:$Constants.TOKEN_LONGITUDE") {

            final Double latitude = pathTokens[Constants.TOKEN_LATITUDE] as Double
            final Double longitude = pathTokens[Constants.TOKEN_LONGITUDE] as Double

            final Integer pageNumberParam

            if (context.request.queryParams.get(Constants.QUERY_PARAM_PAGE)?.isNumber()) {
                pageNumberParam = (context.request.queryParams.get(Constants.QUERY_PARAM_PAGE) as Integer).abs()
            } else {
                pageNumberParam = Constants.DEFAULT_FIRST_PAGE
            }

            placesService
                    .findPlacesNear(latitude, longitude, placesConfig.defaultSearchRadius, pageNumberParam)
                    .single()
                    .subscribe { PaginatedPlaces paginatedPlaces ->

                render(Jackson.json(paginatedPlaces))
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

            final Integer pageNumberParam

            if (context.request.queryParams.get(Constants.QUERY_PARAM_PAGE)?.isNumber()) {
                pageNumberParam = (context.request.queryParams.get(Constants.QUERY_PARAM_PAGE) as Integer).abs()
            } else {
                pageNumberParam = Constants.DEFAULT_FIRST_PAGE
            }

            placesService
                    .findPlacesNear(latitude, longitude, queryRadius, pageNumberParam)
                    .single()
                    .subscribe { PaginatedPlaces paginatedPlaces ->

                render(Jackson.json(paginatedPlaces))
            }
        }

        path(":${Constants.TOKEN_PLACE_ID}") {

            byMethod {

                get {

                    placesService
                            .getPlace(pathTokens[Constants.TOKEN_PLACE_ID])
                            .singleOrDefault(null)
                            .subscribe { IdAssignedPlace place ->

                        place ? render(Jackson.json(place)) : clientError(404)
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
