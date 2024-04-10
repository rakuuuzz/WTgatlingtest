import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class WTgatlingTest extends Simulation {

  FeederBuilder.Batchable<String> csvLogin = csv("data/Login.csv").circular();

  DateTimeFormatter formatter =  DateTimeFormatter.ofPattern("MM/dd/yyyy");
  String datearr = LocalDate.now().plusDays(1).format(formatter);
  String datedepart = LocalDate.now().plusDays(5).format(formatter);


      ChainBuilder HomePage = exec(
      http("HomePage")
        .get("/cgi-bin/welcome.pl?signOff=true")
        .resources(
          http("GetUserSession")
            .get("/cgi-bin/nav.pl?in=home")
            .check(regex("<form method=\"post\" action=\"/cgi-bin/login.pl\" target=\"body\">\n" +
                    "<input type=\"hidden\" name=\"userSession\" value=\"(.+?)\"").saveAs("userSession")
            )
        ),
      pause(5)
      );



      ChainBuilder Login = exec(
      feed(csvLogin),
      http("Login")
        .post("/cgi-bin/login.pl")
        .check(regex("User password was correct"))
        .formParam("userSession", "#{userSession}")
        .formParam("username", "#{login}")
        .formParam("password", "#{password}")
        .resources(
            http("NavBar")
                .get("/cgi-bin/nav.pl?page=menu&in=home").check(regex("WebTours/images/flights")),
            http("CheckLogin")
                .get("/cgi-bin/login.pl?intro=true")
                .check(regex("Welcome, <b>#{login}</b>"))
        ),
      pause(5)
      );
    ChainBuilder go_to_FlightPage = exec(
      http("go_to_FlightsPage")
        .get("/cgi-bin/welcome.pl?page=search")
            .check(regex(" User has returned to the search page."))
        .resources(
          http("ChooseCity")
            .get("/cgi-bin/reservations.pl?page=welcome")
            .check(regex("<option value=\"(.*?)\"").findAll().saveAs("CityArr")),
          http("request_10")
            .get("/cgi-bin/nav.pl?page=menu&in=flights")
        ),
            pause(5)

    .exec(session -> {
        List<String> cityArr = (List<String>) session.get("CityArr");
        Random rand = new Random();
        int rndarr = rand.nextInt(cityArr.size()/2);
        String citydepart = cityArr.get(rndarr);
        String cityarrive = cityArr.get((cityArr.size()/2-1)-rndarr);


        return session
            .set("citydepart",citydepart)
            .set("cityarrive",cityarrive);
          }));
    ChainBuilder ReservationPage = exec(
      http("ReservationPage")
        .post("/cgi-bin/reservations.pl")
        .check(
            regex("name=\"outboundFlight\" value=\"(.*?)\"").findAll().saveAs("outboundFlights")
        )
        .formParam("advanceDiscount", "0")
        .formParam("depart", "#{citydepart}")
        .formParam("departDate", datearr)
        .formParam("arrive", "#{cityarrive}")
        .formParam("returnDate", datedepart)
        .formParam("numPassengers", "1")
        .formParam("seatPref", "#{seatPref}")
        .formParam("seatType", "#{seatType}")
        .formParam("findFlights.x", "73")
        .formParam("findFlights.y", "9")
        .formParam(".cgifields", "roundtrip")
        .formParam(".cgifields", "seatType")
        .formParam(".cgifields", "seatPref"),
        pause(5)

          .exec(session -> {
              List<String> outboundFlights = (List<String>) session.get("outboundFlights");
              Random rand = new Random();
              int rndarr = rand.nextInt(outboundFlights.size());
              String outboundFlight_1 = outboundFlights.get(rndarr);

              return session
                .set("outboundFlight",outboundFlight_1);
          }));
    ChainBuilder ConfReservation = exec(
      http("ConfReservation")
        .post("/cgi-bin/reservations.pl")
        .check(regex("Payment Details"))
        .formParam("outboundFlight", "#{outboundFlight}")
        .formParam("numPassengers", "1")
        .formParam("advanceDiscount", "0")
        .formParam("seatType", "#{seatType}")
        .formParam("seatPref", "#{seatPref}")
        .formParam("reserveFlights.x", "54")
        .formParam("reserveFlights.y", "9"),
        pause(5)
    );
    ChainBuilder LastChooseReservation = exec(
      http("LastChooseReservation")
        .post("/cgi-bin/reservations.pl")
        .check(regex(" #{citydepart} to #{cityarrive}."))
        .formParam("firstName", "#{firstName}")
        .formParam("lastName", "#{lastName}")
        .formParam("address1", "#{street}")
        .formParam("address2", "#{city}")
        .formParam("pass1", " #{firstName} #{lastName}")
        .formParam("creditCard", "#{creditCard}")
        .formParam("expDate", "#{expDate}")
        .formParam("oldCCOption", "")
        .formParam("numPassengers", "1")
        .formParam("seatType", "#{seatType}")
        .formParam("seatPref", "#{seatPref}")
        .formParam("outboundFlight", "#{outboundFlight}")
        .formParam("advanceDiscount", "0")
        .formParam("returnFlight", "")
        .formParam("JSFormSubmit", "off")
        .formParam("buyFlights.x", "62")
        .formParam("buyFlights.y", "9")
        .formParam(".cgifields", "saveCC")
    );

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:1080")
            .inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*\\.svg", ".*detectportal\\.firefox\\.com.*"));

    private ScenarioBuilder scn = scenario("WTgatlingTest").exec(HomePage,Login,go_to_FlightPage,ReservationPage,ConfReservation,LastChooseReservation);

    {
        setUp(
                scn.injectOpen(
                    atOnceUsers(1)

                )
        ).protocols(httpProtocol);
    }
}
