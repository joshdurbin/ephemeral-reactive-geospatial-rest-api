package io.durbs.rtree.places

import com.github.davidmoten.grumpy.core.Position
import com.github.davidmoten.rtree.Entry
import com.github.davidmoten.rtree.Factories
import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Geometry
import com.github.davidmoten.rtree.geometry.Point
import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.rtree.places.domain.IdAssignedPlace
import io.durbs.rtree.places.domain.Place
import io.durbs.rtree.places.domain.PlaceWithDistance
import io.durbs.rtree.places.error.NoSuchPlaceException
import rx.Observable
import rx.functions.Action1
import rx.functions.Func1
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

    volatile RTree<IdAssignedPlace, Point> tree

    @Inject
    RTreePlacesService(PlacesConfig placesConfig) {

        this.placesConfig = placesConfig

        RTree.Builder rTreeBuilder = RTree.minChildren(placesConfig.minChildren).maxChildren(placesConfig.maxChildren)

        if (placesConfig.star) {
            rTreeBuilder = rTreeBuilder.star()
        }

        tree = rTreeBuilder.create()

        rTreeActionPublishSubject = PublishSubject.create()
        rTreeActionPublishSubject.subscribe { MutatingRTreeAction rTreeAction ->

            if (rTreeAction.type == MutatingRTreeAction.Type.INSERT) {
                tree = tree.add(rTreeAction.data)
            } else if (rTreeAction.type == MutatingRTreeAction.Type.DELETE) {
                tree = tree.delete(rTreeAction.data)
            } else if (rTreeAction.type == MutatingRTreeAction.Type.REBUILD) {
                tree = rTreeBuilder.create()
            }

        } as Action1
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

        tree.entries()
            .filter({ Entry<IdAssignedPlace, Point> entry ->

                entry.value().id == id
            } as Func1)
            .map({ Entry<IdAssignedPlace, Point> entry ->

                entry.value()
            })
            .bindExec()
    }

    @Override
    Observable<IdAssignedPlace> getRandomPlace() {

        if (tree.empty) {

            throw new NoSuchPlaceException()

        } else {

            tree.entries()
                    .elementAt(new Random().nextInt(tree.size()))
                    .map({ Entry<IdAssignedPlace, Point> entry ->

                entry.value()
            } as Func1)
            .bindExec()
        }
    }

    @Override
    Observable<IdAssignedPlace> getAllPlaces() {

        tree.entries().map({ Entry<IdAssignedPlace, Point> entry ->

            entry.value()
        } as Func1)
        .limit(placesConfig.maxResults)
        .bindExec()
    }

    @Override
    Integer getNumberOfStoredPlaces() {

        tree.size()
    }

    @Override
    Observable<PlaceWithDistance> findPlacesNear(Double latitude, Double longitude, Double searchRadius) {

        final Position queryPosition = Position.create(latitude, longitude)

        final Position north = queryPosition.predict(searchRadius, 0)
        final Position south = queryPosition.predict(searchRadius, 180)
        final Position east = queryPosition.predict(searchRadius, 90)
        final Position west = queryPosition.predict(searchRadius, 270)

        final Geometry searchArea = Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat())

        tree.search(searchArea)
            .filter({ Entry<IdAssignedPlace, Point> entry ->

                queryPosition.getDistanceToKm(Position.create(entry.geometry().y(), entry.geometry().x())) < searchRadius

            } as Func1)
            .limit(placesConfig.maxResults)
            .map({ Entry<IdAssignedPlace, Point> entry ->

                final Double distance = queryPosition.getDistanceToKm(Position.create(entry.geometry().y(), entry.geometry().x()))
                final Double roundedDistance = distance.round(Constants.DISTANCE_ROUNDING_DECIMAL_PLACES)

                new PlaceWithDistance(distance: roundedDistance,
                        place: entry.value())
            } as Func1)
            .bindExec()
    }
}
