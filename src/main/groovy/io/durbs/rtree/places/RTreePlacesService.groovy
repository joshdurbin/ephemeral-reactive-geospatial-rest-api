package io.durbs.rtree.places

import com.github.davidmoten.grumpy.core.Position
import com.github.davidmoten.rtree.Entry
import com.github.davidmoten.rtree.Factories
import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Point
import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.rtree.places.domain.IdAssignedPlace
import io.durbs.rtree.places.domain.PaginatedPlaces
import io.durbs.rtree.places.domain.Place
import io.durbs.rtree.places.domain.PlaceWithDistance
import rx.Observable
import rx.functions.Action1
import rx.functions.Func1
import rx.functions.Func2
import rx.subjects.PublishSubject

@Slf4j
@CompileStatic
@Singleton
class RTreePlacesService implements PlacesService {

    @Canonical
    @CompileStatic
    static class MutatingRTreeAction {

        static enum Type {

            INSERT,
            DELETE,
            REBUILD
        }

        Entry<IdAssignedPlace,Point> data
        Type type
    }

    final PublishSubject<MutatingRTreeAction> rTreeActionPublishSubject

    final PlacesConfig placesConfig
    final RTree.Builder rTreeBuilder

    volatile RTree<IdAssignedPlace, Point> tree

    @Inject
    RTreePlacesService(PlacesConfig placesConfig, RTree.Builder rTreeBuilder) {

        this.placesConfig = placesConfig
        this.rTreeBuilder = rTreeBuilder

        tree = rTreeBuilder.create()

        rTreeActionPublishSubject = PublishSubject.create()
        rTreeActionPublishSubject.subscribe({ MutatingRTreeAction rTreeAction ->

            if (rTreeAction.type == MutatingRTreeAction.Type.INSERT) {
                tree = tree.add(rTreeAction.data)
            } else if (rTreeAction.type == MutatingRTreeAction.Type.DELETE) {
                tree = tree.delete(rTreeAction.data)
            } else if (rTreeAction.type == MutatingRTreeAction.Type.REBUILD) {
                tree = rTreeBuilder.create()
            }

        } as Action1)
    }

    @Override
    void removeAllPlaces() {

        rTreeActionPublishSubject.onNext(new MutatingRTreeAction(type: MutatingRTreeAction.Type.REBUILD))
    }

    @Override
    void removePlace(String id) {

        getPlace(id).subscribe { IdAssignedPlace idAssignedPlace ->

            rTreeActionPublishSubject.onNext(new MutatingRTreeAction(type: MutatingRTreeAction.Type.DELETE,
                    data: Factories.defaultFactory().createEntry(idAssignedPlace,
                            Geometries.pointGeographic(idAssignedPlace.place.longitude, idAssignedPlace.place.latitude))))
        } as Action1
    }

    @Override
    Observable<String> savePlace(Place place) {

        final String id = UUID.randomUUID().toString()

        Entry<IdAssignedPlace,Point> entryToInsert = Factories.defaultFactory().createEntry(new IdAssignedPlace(id: id, place: place),
                Geometries.pointGeographic(place.longitude, place.latitude))

        rTreeActionPublishSubject.onNext(new MutatingRTreeAction(type: MutatingRTreeAction.Type.INSERT, data: entryToInsert))

        Observable.just(id).bindExec()
    }

    @Override
    Observable<IdAssignedPlace> getPlace(String id) {

        tree.entries().filter({ Entry<IdAssignedPlace, Point> entry ->
            entry.value().id == id
        } as Func1)
        .map({ Entry<IdAssignedPlace, Point> entry ->
            entry.value()
        } as Func1)
        .bindExec()
    }

    @Override
    Observable<IdAssignedPlace> getRandomPlace() {

        Observable<IdAssignedPlace> randomPlaceObservable

        if (tree.empty) {
            randomPlaceObservable = Observable.empty()
        } else {
            randomPlaceObservable = tree.entries()
                    .elementAt(new Random().nextInt(tree.size()))
                    .map({ Entry<IdAssignedPlace, Point> entry ->
                entry.value()
            } as Func1)
        }

        randomPlaceObservable.bindExec()
    }

    @Override
    Observable<PaginatedPlaces<IdAssignedPlace>> getAllPlaces(Integer pageNumber) {

        final Observable<IdAssignedPlace> getAllIdAssignedPlacesObservable = tree
            .entries()
            .map({ Entry<IdAssignedPlace, Point> entry ->
                entry.value()
            } as Func1)
            .cacheWithInitialCapacity(placesConfig.findAllObservableCacheInitialCapacity)

        final Observable<Integer> totalNumberOfAllIdAssignedPlacesObservable = getAllIdAssignedPlacesObservable.count()

        final Observable<List<IdAssignedPlace>> paginatedIdAssignedPlacesObservable = getAllIdAssignedPlacesObservable
            .skip(placesConfig.maxResultsPerPage * pageNumber)
            .limit(placesConfig.maxResultsPerPage)
            .toList()

        Observable.zip(totalNumberOfAllIdAssignedPlacesObservable, paginatedIdAssignedPlacesObservable, { Integer count, List<PlaceWithDistance> places ->

            new PaginatedPlaces<IdAssignedPlace>(totalPlaces: count, places: places)
        })
        .bindExec()
    }

    @Override
    Observable<PaginatedPlaces<PlaceWithDistance>> findPlacesNear(Double latitude, Double longitude, Double searchRadius, Integer pageNumber) {

        final Position queryPosition = Position.create(latitude, longitude)

        final Position north = queryPosition.predict(searchRadius, 0)
        final Position south = queryPosition.predict(searchRadius, 180)
        final Position east = queryPosition.predict(searchRadius, 90)
        final Position west = queryPosition.predict(searchRadius, 270)

        final Observable<PlaceWithDistance> queryObservable = tree.search(Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat()))
            .map({ Entry<IdAssignedPlace, Point> entry ->
                new PlaceWithDistance(
                    distance: queryPosition.getDistanceToKm(Position.create(entry.geometry().y(), entry.geometry().x())).round(Constants.DISTANCE_ROUNDING_DECIMAL_PLACES),
                    place: entry.value())
            } as Func1)
            .filter({ PlaceWithDistance placeWithDistance ->
                placeWithDistance.distance < searchRadius
            } as Func1)
            .cacheWithInitialCapacity(placesConfig.findNearObservableCacheInitialCapacity)

        final Observable<PlaceWithDistance> sortedPlaceWithDistanceObservable = queryObservable
            .sorted({ PlaceWithDistance first, PlaceWithDistance second ->
                Double.compare(first.distance, second.distance)
            } as Func2)
            .skip(placesConfig.maxResultsPerPage * pageNumber)
            .limit(placesConfig.maxResultsPerPage)

        Observable.zip(queryObservable.count(), sortedPlaceWithDistanceObservable.toList(), { Integer count, List<PlaceWithDistance> places ->

            new PaginatedPlaces<PlaceWithDistance>(totalPlaces: count, places: places)
        })
        .bindExec()
    }
}
