package service.csv

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting

/**
  * @author Sebastian Hardt (s.hardt@micromata.de)
  *         Date: 03.07.19
  *         Time: 16:04
  */
class ArmyCSVDataParserSpec extends PlaySpec with GuiceOneAppPerTest with Injecting  {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .build()


  val armyCsvDataparser: ArmyCSVDataParser = fakeApplication().injector.instanceOf(classOf[ArmyCSVDataParser])

  "ArmyCSVDataParser" should {
    "parse csv" in {
      val factionsOption = armyCsvDataparser.parseData()
      factionsOption.isEmpty mustBe false
      val factions = factionsOption.get
      factions.size mustBe 1
      factions.exists(_.name == "Not a real faction name") mustBe false
      val marauders = factions.find(_.name == "Orc Marauders")
      marauders.isEmpty mustBe false
      marauders.get.troops.size mustBe 16

    }
  }

}
