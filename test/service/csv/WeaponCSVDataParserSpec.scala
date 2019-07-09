package service.csv

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

/**
  * Test for the weapon csv data parser
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class WeaponCSVDataParserSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {


  val weaponCsvDataParser: WeaponCsvDataParser = fakeApplication().injector.instanceOf(classOf[WeaponCsvDataParser])

  "WeaponCsvDataParser" should {
    "parse csv" in {
      val weaponCsvData = weaponCsvDataParser.parseData()
      weaponCsvData mustBe defined
      val weapons = weaponCsvData.get
      weapons.size > 1 mustBe true
      weapons.exists(_.name == "asdasd") mustBe false
    }
  }

}
