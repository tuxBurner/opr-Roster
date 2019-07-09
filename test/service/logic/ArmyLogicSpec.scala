package service.logic

import java.util.UUID

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import service.DataInitializer

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
  * Tests for the army logic
  */
class ArmyLogicSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {


  val armyLogic: ArmyLogic = fakeApplication.injector.instanceOf(classOf[ArmyLogic])
  val dataInitalizer: DataInitializer = fakeApplication.injector.instanceOf(classOf[DataInitializer])

  dataInitalizer.initData()

  val orcMarauderFaction = "Orc Marauders"

  "ArmyLogic" should {

    "return empty when army is not present" in {
      val armyNotKnown = awaitResult(armyLogic.getArmy(uuid = UUID.randomUUID().toString))
      armyNotKnown mustBe empty
    }

    "return empty when the faction name is not set correctly" in {
      val factionNotKnown = awaitResult(armyLogic.addNewArmy(UUID.randomUUID().toString, "FactionNotKnow"))
      factionNotKnown mustBe empty

    }

    s"return an army of $orcMarauderFaction when adding an army" in {
      val factionKnown = awaitResult(armyLogic.addNewArmy(UUID.randomUUID().toString, orcMarauderFaction))
      factionKnown mustBe defined
      factionKnown.get.factionName mustBe orcMarauderFaction
    }

    s"return army with no troops when adding not known troop" in {
      val uuid = UUID.randomUUID().toString
      val factionKnown = awaitResult(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      factionKnown mustBe defined
      val notUpdatedArmy = awaitResult(armyLogic.addTroopToArmy(uuid, "TroopNotKnown"))
      notUpdatedArmy mustBe defined
      notUpdatedArmy.get.troops.length mustBe 0
    }

    s"Cannot add troop to unknown army" in {
      val updatedArmy = awaitResult(armyLogic.addTroopToArmy(UUID.randomUUID().toString, "Warlord"))
      updatedArmy mustBe empty
    }

    s"return army when troop is known in the faction" in {
      val uuid = UUID.randomUUID().toString
      val factionKnown = awaitResult(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      factionKnown mustBe defined
      val updatedArmy = awaitResult(armyLogic.addTroopToArmy(factionKnown.get.uuid, "Warlord"))
      updatedArmy mustBe defined
      updatedArmy.get.troops.length mustBe 1
      updatedArmy.get.uuid mustBe uuid

      // check if the cache got also updated
      val armyFromCache = awaitResult(armyLogic.getArmy(updatedArmy.get.uuid))
      armyFromCache mustBe defined
      armyFromCache.get.troops.length mustBe 1
      armyFromCache.get.uuid mustBe uuid
      armyFromCache.get.troops(0).uuid mustBe updatedArmy.get.troops(0).uuid
    }
  }

  /**
    * Helper function which waits 500 millis to get the result
    *
    * @param futureFun the function which returns a [[Future]] of the type [[T]]
    * @tparam T the type the function returns
    * @return the result of the [[Future]]
    */
  private def awaitResult[T](futureFun: Future[T]): T = {
    Await.result(futureFun, 500.millis)
  }


}
