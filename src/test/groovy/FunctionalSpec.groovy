import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.durbs.rtree.places.Constants
import io.durbs.rtree.places.domain.Place
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.http.MediaType
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class FunctionalSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest applicationUnderTest = new GroovyRatpackMainApplicationUnderTest()

    @Shared
    ObjectMapper objectMapper = new ObjectMapper()

    @Shared
    JsonSlurper jsonSlurper = new JsonSlurper()

    static Place exploratorium
    static Place sutroBaths
    static Place twinPeaks

    static Double distanceBetweenExploratoriumAndSutroBathsInKilometers = 9.91

    static {
        exploratorium = new Place(name: 'Exploratorium',
                address: 'Pier 15 The Embarcadero',
                city: 'San Francisco',
                state: 'CA',
                zipCode: '94111',
                telephoneNumber: '415-528-4444',
                latitude: 37.7964082,
                longitude: -122.4016394)

        sutroBaths = new Place(name: 'Sutro Baths',
                address: '1004 Point Lobos Ave',
                city: 'San Francisco',
                state: 'CA',
                zipCode: '94121',
                telephoneNumber: '415-426-5240',
                latitude: 37.7788151,
                longitude: -122.512226)

        twinPeaks = new Place(latitude: 37.7696723,
                longitude: -122.4457446)
    }

    void "start and return zero places"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 0
    }

    void "fail to insert the twin peaks place without a name"() {

        when:
        def response = applicationUnderTest.httpClient.requestSpec { spec ->

            spec.headers.'Content-Type' = [MediaType.APPLICATION_JSON]
            spec.body { body ->
                body.text(objectMapper.writeValueAsString(twinPeaks))
            }
        }.post(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        response.statusCode == 400
        jsonSlurper.parseText(response.body.text).fieldErrors.collect { it.field }.contains('name')
    }

    void "fail to insert a place with no body, no data missing the required named, lat, and long"() {

        when:
        def response = applicationUnderTest.httpClient.requestSpec { spec ->

            spec.headers.'Content-Type' = [MediaType.APPLICATION_JSON]
            spec.body { body ->
                body.text('{}')
            }
        }.post(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        response.statusCode == 400
        jsonSlurper.parseText(response.body.text).fieldErrors.collect { it.field }.contains('name')
        jsonSlurper.parseText(response.body.text).fieldErrors.collect { it.field }.contains('latitude')
        jsonSlurper.parseText(response.body.text).fieldErrors.collect { it.field }.contains('longitude')
    }

    void "get a random place with an empty tree"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/random")

        then:
        response.statusCode == 400
    }

    void "insert the exploratorium place"() {

        when:
        def createResponse = applicationUnderTest.httpClient.requestSpec { spec ->

            spec.headers.'Content-Type' = [MediaType.APPLICATION_JSON]
            spec.body { body ->
                body.text(objectMapper.writeValueAsString(exploratorium))
            }
        }.post(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        createResponse.statusCode == 200

        when:
        def id = jsonSlurper.parseText(createResponse.body.text).id
        def getResponse = applicationUnderTest.httpClient.get("${Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH}/${id}")

        then:
        getResponse.statusCode == 200
        jsonSlurper.parseText(getResponse.body.text).id == id
        jsonSlurper.parseText(getResponse.body.text).name == exploratorium.name
    }

    void "count should be one after exploratorium insertion"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 1
    }

    void "the single random place should be equal to the exploratorium"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/random")

        then:
        objectMapper.readValue(response.body.text, Place) == exploratorium
    }

    void "insert the sutro baths place"() {

        when:
        def createResponse = applicationUnderTest.httpClient.requestSpec { spec ->

            spec.headers.'Content-Type' = [MediaType.APPLICATION_JSON]
            spec.body { body ->
                body.text(objectMapper.writeValueAsString(sutroBaths))
            }
        }.post(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        createResponse.statusCode == 200

        when:
        def id = jsonSlurper.parseText(createResponse.body.text).id
        def getResponse = applicationUnderTest.httpClient.get("${Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH}/${id}")

        then:
        getResponse.statusCode == 200
        jsonSlurper.parseText(getResponse.body.text).id == id
        jsonSlurper.parseText(getResponse.body.text).name == sutroBaths.name
    }

    void "count should be two after exploratorium, sutro baths insertion"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 2
    }

    void "1km geo query from twin peaks"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/near/${twinPeaks.latitude}/${twinPeaks.longitude}/1")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 0
    }

    void "10km geo query from the exploratorium should return itself and sutro baths"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/near/${exploratorium.latitude}/${exploratorium.longitude}/10")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 2
        (jsonSlurper.parseText(response.body.text).places as List).first().distance == 0
        (jsonSlurper.parseText(response.body.text).places as List).first().name == exploratorium.name
        (jsonSlurper.parseText(response.body.text).places as List).last().distance == distanceBetweenExploratoriumAndSutroBathsInKilometers
        (jsonSlurper.parseText(response.body.text).places as List).last().name == sutroBaths.name
    }

    void "10km geo query from sutro baths should return itself and the exploratorium"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/near/${sutroBaths.latitude}/${sutroBaths.longitude}/10")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 2
        (jsonSlurper.parseText(response.body.text).places as List).first().distance == 0
        (jsonSlurper.parseText(response.body.text).places as List).first().name == sutroBaths.name
        (jsonSlurper.parseText(response.body.text).places as List).last().distance == distanceBetweenExploratoriumAndSutroBathsInKilometers
        (jsonSlurper.parseText(response.body.text).places as List).last().name == exploratorium.name
    }

    void "5km geo query from the exploratorium should return itself only"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/near/${exploratorium.latitude}/${exploratorium.longitude}/5")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 1
        (jsonSlurper.parseText(response.body.text).places as List).first().distance == 0
        (jsonSlurper.parseText(response.body.text).places as List).first().name == exploratorium.name
    }

    void "5km geo query from sutro baths should return itself only"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/near/${sutroBaths.latitude}/${sutroBaths.longitude}/5")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 1
        (jsonSlurper.parseText(response.body.text).places as List).first().distance == 0
        (jsonSlurper.parseText(response.body.text).places as List).first().name == sutroBaths.name
    }

    void "5km geo query twin peaks should return the exploratorium"() {

        when:
        def response = applicationUnderTest.httpClient.get("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/near/${twinPeaks.latitude}/${twinPeaks.longitude}/5")

        then:
        jsonSlurper.parseText(response.body.text).totalPlaces == 1
        (jsonSlurper.parseText(response.body.text).places as List).first().name == exploratorium.name
    }

    void "delete a single place"() {

        when:
        def getPlacesResponse = applicationUnderTest.httpClient.get(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        jsonSlurper.parseText(getPlacesResponse.body.text).totalPlaces == 2

        when:
        def placeIdToDelete = (jsonSlurper.parseText(getPlacesResponse.body.text).places as List).first().id
        applicationUnderTest.httpClient.delete("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/${placeIdToDelete}")
        def getPlacesResponseFollowingDeletion = applicationUnderTest.httpClient.get(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        jsonSlurper.parseText(getPlacesResponseFollowingDeletion.body.text).totalPlaces == 1
        (jsonSlurper.parseText(getPlacesResponseFollowingDeletion.body.text).places as List).first().id != placeIdToDelete
    }

    void "delete all places"() {

        when:
        def getPlacesResponse = applicationUnderTest.httpClient.get(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        jsonSlurper.parseText(getPlacesResponse.body.text).totalPlaces == 1

        when:
        applicationUnderTest.httpClient.delete("$Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH/deleteAllPlaces")
        def getPlacesResponseFollowingDeletion = applicationUnderTest.httpClient.get(Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH)

        then:
        jsonSlurper.parseText(getPlacesResponseFollowingDeletion.body.text).totalPlaces == 0
    }

    void "request to get individual place which does not exist should fail with a 404"() {

        when:
        def response = applicationUnderTest.httpClient.get("${Constants.BASE_API_RESOURCE_PATH_WITH_STARTING_SLASH}/asdf")

        then:
        response.statusCode == 404
    }
}
