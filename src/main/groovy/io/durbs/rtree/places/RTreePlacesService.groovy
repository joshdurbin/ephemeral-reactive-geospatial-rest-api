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
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.rtree.places.domain.IdAssignedPlace
import io.durbs.rtree.places.domain.Place
import io.durbs.rtree.places.domain.PlaceWithDistance
import io.durbs.rtree.places.error.NoSuchPlaceException
import rx.Observable
import rx.Subscriber
import rx.functions.Func1
import rx.subjects.PublishSubject

@Slf4j
@CompileStatic
@Singleton
class RTreePlacesService implements PlacesService {

    final PublishSubject<Entry<IdAssignedPlace,Point>> subject
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

        subject = PublishSubject.create()
        subject.subscribe(new Subscriber<Entry<IdAssignedPlace,Point>>() {

            @Override
            void onCompleted() {

            }

            @Override
            void onError(Throwable exception) {

                log.error(exception.getMessage(), exception)
            }

            @Override
            void onNext(final Entry<IdAssignedPlace, Point> entry) {

                log.debug("adding place '${entry.value().name}' w/ coordinates [${entry.value().latitude},${entry.value().longitude}]")
                tree = tree.add(entry)
            }
        })
    }

    @Override
    Observable<String> savePlace(Place place) {

        final String id = UUID.randomUUID().toString()

        subject.onNext(Factories.defaultFactory().createEntry(new IdAssignedPlace(id: id, place: place), Geometries.pointGeographic(place.longitude, place.latitude)))

        Observable.defer({

            Observable.just(id)
        }).bindExec()
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
    Observable<Integer> getNumberOfStoredPlaces() {

        Observable.defer({

            Observable.just(tree.size())
        }).bindExec()
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
