package service

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting

/**
  * Test for the abilities csv data parser
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class DataInitializerspec extends PlaySpec with GuiceOneAppPerTest with Injecting  {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .build()

  val abilitiesCsvDataParser: AbilitiesCsvDataParser = fakeApplication().injector.instanceOf(classOf[AbilitiesCsvDataParser])

  "AbilitiesCsvDataParser" should {
    "parse csv" in {
      val abilitiesCsvData = abilitiesCsvDataParser.parseData()
      abilitiesCsvData.isEmpty mustBe false
      val abilities = abilitiesCsvData.get
      abilities.size > 0 mustBe true
    }
  }

}
