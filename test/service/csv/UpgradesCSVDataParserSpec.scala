package service.csv

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting

/**
  * Test for the weapon csv data parser
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class UpgradesCSVDataParserSpec extends PlaySpec with GuiceOneAppPerTest with Injecting  {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .build()

  val upgradesCsvDataParser: UpgradesCsvDataParser = fakeApplication().injector.instanceOf(classOf[UpgradesCsvDataParser])

  "UpgradesCsvDataParser" should {
    "parse csv" in {
      val weaponCsvData = upgradesCsvDataParser.parseData()
      weaponCsvData.isEmpty mustBe false
    }
  }

}
