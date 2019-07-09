package service.csv

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

/**
  * Test for the abilities csv data parser
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class AbilitiesCSVDataParserSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {


  val abilitiesCsvDataParser: AbilitiesCsvDataParser = fakeApplication().injector.instanceOf(classOf[AbilitiesCsvDataParser])

  "AbilitiesCsvDataParser" should {
    "parse csv" in {
      val abilitiesCsvData = abilitiesCsvDataParser.parseData()
      abilitiesCsvData mustBe defined
      val abilities = abilitiesCsvData.get
      abilities.size > 0 mustBe true
    }
  }

}
