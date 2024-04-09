import io.gatling.core.Predef.scenario
import Request._

object Simulation {
  def getOrderScen = scenario("Scen")
    .exec(getOrders)
}
