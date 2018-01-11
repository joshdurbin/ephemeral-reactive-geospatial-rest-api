package io.durbs.rtree.places

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davidmoten.rtree.RTree
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.validation.Validation
import javax.validation.Validator

@Slf4j
@CompileStatic
class PlacesModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(PlacesService).to(RTreePlacesService)
        bind(PlacesHandlerChain)
    }

    @Provides
    @Singleton
    Validator provideValidator() {
        Validation.buildDefaultValidatorFactory().validator
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper() {
        new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    @Provides
    @Singleton
    RTree.Builder provideBuilder(PlacesConfig placesConfig) {

        RTree.Builder rTreeBuilder = RTree.minChildren(placesConfig.minChildren).maxChildren(placesConfig.maxChildren)

        if (placesConfig.star) {
            rTreeBuilder = rTreeBuilder.star()
        }

        rTreeBuilder
    }
}
