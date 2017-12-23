package io.durbs.rtree.places

import com.google.inject.AbstractModule
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class PlacesModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(PlacesService).to(RTreePlacesService)
        bind(PlacesHandlerChain)
    }
}
