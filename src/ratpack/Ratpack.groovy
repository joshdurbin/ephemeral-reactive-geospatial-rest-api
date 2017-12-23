import io.durbs.rtree.places.Constants
import io.durbs.rtree.places.PlacesConfig
import io.durbs.rtree.places.PlacesHandlerChain
import io.durbs.rtree.places.PlacesModule
import ratpack.rx.RxRatpack

import static ratpack.groovy.Groovy.ratpack

ratpack {

  serverConfig {
    yaml('application.yaml')
    require('', PlacesConfig)
  }

  bindings {

    RxRatpack.initialize()
    module PlacesModule
  }

  handlers {

    prefix(Constants.BASE_API_RESOURCE_PATH, PlacesHandlerChain)
  }
}