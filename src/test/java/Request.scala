import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Request {
  def getOrders = {
    exec(
      http(requestName = "getOrders")
        .get("/")
        .check(status.is(200))
    )
  }

}
