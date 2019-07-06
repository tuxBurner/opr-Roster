package service

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import service.models.{AbilitiesDao, FactionsDao}

/**
  * Test for the data initializer
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class DataInitializerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting  {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .build()

  val dataInitializer: DataInitializer = fakeApplication().injector.instanceOf(classOf[DataInitializer])

  "DataInitializer" should {
    "init" in {

      dataInitializer.initData()

      //AbilitiesDao.abilities.length > 0 mustBe true
      FactionsDao.getAllFactionNamesOrderd().length > 0  mustBe true
    }
  }

}
