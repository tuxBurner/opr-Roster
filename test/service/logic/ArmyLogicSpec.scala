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
      testFResultUndefined(armyLogic.getArmy(uuid = UUID.randomUUID().toString))
    }

    "return empty when the faction name is not set correctly" in {
      testFResultUndefined(armyLogic.addNewArmy(UUID.randomUUID().toString, "FactionNotKnow"))
    }

    s"return an army of $orcMarauderFaction when adding an army" in {
      val factionKnown = getFResultDefined(armyLogic.addNewArmy(UUID.randomUUID().toString, orcMarauderFaction))
      factionKnown.factionName mustBe orcMarauderFaction
    }

    s"return army with no troops when adding not known troop" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))

      val notUpdatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "TroopNotKnown"))
      notUpdatedArmy.troops.length mustBe 0
    }

    s"Cannot add troop to unknown army" in {
      testFResultUndefined(armyLogic.addTroopToArmy(UUID.randomUUID().toString, "Warlord"))
    }

    s"return army when troop is known in the faction" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))

      val updatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Warlord"))
      updatedArmy.troops.length mustBe 1
      updatedArmy.uuid mustBe uuid

      // check if the cache got also updated
      val armyFromCache = getFResultDefined(armyLogic.getArmy(uuid))
      armyFromCache.troops.length mustBe 1
      armyFromCache.uuid mustBe uuid
      armyFromCache.troops(0).uuid mustBe updatedArmy.troops(0).uuid

      armyFromCache.totalCosts mustBe 50
    }

    s"updating amount for troop does not work when army is not found" in {
      testFResultUndefined(armyLogic.setTroopAmount("notknown","asdasasd",amount = 12))
    }

    s"update amount of troop does not work when troop was not found" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val notChangedArmy = getFResultDefined(armyLogic.setTroopAmount(uuid,"asdasasd",amount = 12))
      notChangedArmy.troops.length mustBe 0
    }

    s"update amount of troop does not work when amount is 0 or smaller" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))

      val updatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Warlord"))
      updatedArmy.troops.length mustBe 1

      val troopUuid = updatedArmy.troops(0).uuid

      val notChangedArmy = getFResultDefined(armyLogic.setTroopAmount(uuid,troopUuid,amount = 0))
      notChangedArmy.troops.length mustBe 1
      notChangedArmy.troops(0).amount mustBe 1

      val notChangedArmy2 = getFResultDefined(armyLogic.setTroopAmount(uuid,troopUuid,amount = -1))
      notChangedArmy2.troops.length mustBe 1
      notChangedArmy2.troops(0).amount mustBe 1
    }

    s"update amount of troop does work when amount is okay" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))

      val updatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Warlord"))
      updatedArmy.troops.length mustBe 1

      val troopUuid = updatedArmy.troops(0).uuid

      val changedArmy = getFResultDefined(armyLogic.setTroopAmount(uuid,troopUuid,amount = 10))
      changedArmy.troops.length mustBe 1
      changedArmy.troops(0).amount mustBe 10
      changedArmy.totalCosts mustBe 500


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

  private def testFResultUndefined(futureFun: Future[Option[Any]]): Unit = {
    val undfinedOption = awaitResult(futureFun)
    undfinedOption mustBe empty
  }

  private def getFResultDefined[T](futureFun: Future[Option[T]]): T = {
    val optionRes = awaitResult(futureFun)
    optionRes mustBe defined
    optionRes.get
  }


}
