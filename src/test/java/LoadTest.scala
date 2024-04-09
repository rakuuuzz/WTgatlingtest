import io.gatling.core.Predef._
import _root_.Simulation.getOrderScen
import io.gatling.http.Predef.http

class LoadTest  extends Simulation {

  val httpConf = http.baseUrl(url = "http://localhost:1080/webtours/")

  setUp(
    getOrderScen.inject(
      constantUsersPerSec(1) during 1
    ).protocols(httpConf)

  )
}