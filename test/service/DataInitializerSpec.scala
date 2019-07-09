package service

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import service.models.{FactionsDao, TroopDao}

/**
  * Test for the data initializer
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class DataInitializerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting  {


  val dataInitializer: DataInitializer = fakeApplication().injector.instanceOf(classOf[DataInitializer])

  "DataInitializer" should {
    "init" in {

      dataInitializer.initData()

      //AbilitiesDao.abilities.length > 0 mustBe true
      FactionsDao.getAllFactionNamesOrderd().length > 0  mustBe true

      TroopDao.getTroopByNameAndFactionName(troopName = "Warlord", factionName = "Orc Marauders") mustBe defined
    }
  }

}
