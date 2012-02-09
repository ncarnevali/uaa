import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.script.GatlingSimulation

class Simulation extends GatlingSimulation {

  val urlBase = sys.env.getOrElse("GATLING_UAA_BASE", "http://localhost:8080/uaa")

  val httpConf = httpConfig.baseURL(urlBase)

  val plainHeaders = Map(
    "Accept" -> "application/json",
    "Content-Type" -> "application/x-www-form-urlencoded")

  // Feeder which generates the usernames
  val feeder = new Feeder {
    var counter = 0

    def next = {
      println("Counter :" + counter)
      counter += 1
      Map("username" -> ("joe" + counter))
    }
  }

  // Authenticate and create 100 users
  val createUsers = scenario("Create user")
    .feed(feeder)
    .exec((s: Session) => {
      println ("Creating user " + s.getAttribute("username"))
      println ("Session " + s)
      s
    })
    .exec(
    http("getToken")
      .post("/oauth/token")
      .basicAuth("scim", "scimsecret")
      .param("client_id", "scim")
      .param("scope", "write password")
      .param("grant_type", "client_credentials")
      .headers(plainHeaders)
      .check(status.is(200), regex(""""access_token":"(.*?)"""").saveAs("access_token"))
  )
  .exec((s: Session) => {
    println ("Creating user " + s.getAttribute("username"))
    println ("Session " + s)
    s
  })
  .exec(
      http("createUser")
        .post("/User")
        .header("Authorization", "Bearer ${access_token}")
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .body("""{"name":{"givenName":"Joe","familyName":"User","formatted":"Joe User"},"userName":"${username}","emails":[{"value":"${username}@blah.com"}]}""")
        .check(status.is(201))
  )

  runSimulation(createUsers.configure users 20 protocolConfig httpConf)
}
