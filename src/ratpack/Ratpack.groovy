import com.github.gregwhitaker.ratpack.error.ErrorModule
import io.durbs.rtree.places.Constants
import io.durbs.rtree.places.PlacesConfig
import io.durbs.rtree.places.PlacesHandlerChain
import io.durbs.rtree.places.PlacesModule
import ratpack.handling.RequestLogger
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
    module ErrorModule
  }

  handlers {
    all(RequestLogger.ncsa())
    prefix(Constants.BASE_API_RESOURCE_PATH, PlacesHandlerChain)
  }
}