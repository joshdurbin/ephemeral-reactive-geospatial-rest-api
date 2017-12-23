package io.durbs.rtree.places

@Singleton
class Constants {

    static final String TOKEN_LATITUDE = 'latitude'
    static final String TOKEN_LONGITUDE = 'longitude'
    static final String TOKEN_RADIUS = 'radius'
    static final String TOKEN_PLACE_ID = 'id'

    static final Integer FIND_NEAR_DISTANCE_CALCULATION_MULTIPLIER = 100_000

    static final String BASE_API_RESOURCE_PATH = 'api/v0/places'
    static final String BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH = "/${BASE_API_RESOURCE_PATH}"
}
