package service.logic

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import service.DataInitializer

import scala.concurrent.{Await, Future}

import scala.concurrent.duration._

class BasicOprLogicSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val armyLogic: ArmyLogic = fakeApplication.injector.instanceOf(classOf[ArmyLogic])

  val upgradeLogic: UpgradeLogic = fakeApplication.injector.instanceOf(classOf[UpgradeLogic])

  val dataInitalizer: DataInitializer = fakeApplication.injector.instanceOf(classOf[DataInitializer])

  // init the data
  dataInitalizer.initData()

  /**
    * Name of the orc marauders faction
    */
  val orcMarauderFaction = "Orc Marauders"

  /**
    * Name of the orc warlord
    */
  val orcWarlordName = "Warlord"

  /**
    * Helper function which waits 500 millis to get the result
    *
    * @param futureFun the function which returns a [[Future]] of the type [[T]]
    * @tparam T the type the function returns
    * @return the result of the [[Future]]
    */
  protected def awaitResult[T](futureFun: Future[T]): T = {
    Await.result(futureFun, 1500.millis)
  }

  /**
    * Checks if the result of the future is empty
    *
    * @param futureFun the function which returns a [[Future]] of the type [[T]]
    */
  protected def testFResultUndefined(futureFun: Future[Option[Any]]): Unit = {
    val undfinedOption = awaitResult(futureFun)
    undfinedOption mustBe empty
  }

  /**
    * Gets the result of the Future and checks if it is defined
    *
    * @param futureFun the function which returns a [[Future]] of the type [[T]]
    * @tparam T the type the function returns
    * @return the T object from the option
    */
  protected def getFResultDefined[T](futureFun: Future[Option[T]]): T = {
    val optionRes = awaitResult(futureFun)
    optionRes mustBe defined
    optionRes.get
  }

}
