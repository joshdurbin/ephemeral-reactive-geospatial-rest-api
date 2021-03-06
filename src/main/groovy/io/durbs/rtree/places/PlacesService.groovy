package io.durbs.rtree.places

import io.durbs.rtree.places.domain.IdAssignedPlace
import io.durbs.rtree.places.domain.PaginatedPlaces
import io.durbs.rtree.places.domain.Place
import io.durbs.rtree.places.domain.PlaceWithDistance
import rx.Observable

interface PlacesService {

    /**
     *
     * Destroys and re-creates the RTree.
     *
     */
    void removeAllPlaces()

    /**
     *
     * Removes a place by its generated UUID.
     *
     * @param id
     */
    void removePlace(String id)

    /**
     *
     * Saves a place in the ephemeral, in-memory rtree.
     *
     * @param place
     * @return
     */
    Observable<String> savePlace(Place place)

    /**
     *
     * Returns a place given its id
     *
     * @param id
     * @return
     */
    Observable<IdAssignedPlace> getPlace(String id)

    /**
     *
     * @return
     */
    Observable<IdAssignedPlace> getRandomPlace()

    /**
     *
     * Gets all places in the ephemeral, in-memory rtree.
     *
     * @param pageNumber
     * @return
     */
    Observable<PaginatedPlaces<IdAssignedPlace>> getAllPlaces(Integer pageNumber)

    /**
     *
     * Finds places near a point in the ephemeral, in-memory rtree, with distance in meters.
     *
     * @param latitude
     * @param longitude
     * @param searchRadius
     * @param pageNumber
     * @return
     */
    Observable<PaginatedPlaces<PlaceWithDistance>> findPlacesNear(Double latitude, Double longitude, Double searchRadius, Integer pageNumber)

}