package service.csv

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

/**
  * @author Sebastian Hardt (s.hardt@micromata.de)
  *         Date: 03.07.19
  *         Time: 16:04
  */
class ArmyCSVDataParserSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {


  val armyCsvDataparser: ArmyCSVDataParser = fakeApplication().injector.instanceOf(classOf[ArmyCSVDataParser])

  "ArmyCSVDataParser" should {
    "parse csv" in {
      val factionsOption = armyCsvDataparser.parseData()
      factionsOption mustBe defined
      val factions = factionsOption.get
      factions.size mustBe 2
      val notAFaction = factions.find(_.name == "Not a real faction name")
      notAFaction mustBe empty
      val marauders = factions.find(_.name == "Orc Marauders")
      marauders mustBe defined
      marauders.get.troops.size mustBe 16

    }
  }

}
