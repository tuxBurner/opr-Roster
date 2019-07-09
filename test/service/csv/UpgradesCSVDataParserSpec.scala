package service.csv

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

/**
  * Test for the weapon csv data parser
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class UpgradesCSVDataParserSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val upgradesCsvDataParser: UpgradesCsvDataParser = fakeApplication().injector.instanceOf(classOf[UpgradesCsvDataParser])

  "UpgradesCsvDataParser" should {
    "parse csv" in {
      val upgradesCsvData = upgradesCsvDataParser.parseData()
      upgradesCsvData mustBe defined
      val upgrades = upgradesCsvData.get
      upgrades.size > 0 mustBe true
    }
  }

}
