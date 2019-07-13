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
      val troopAmount = 10
      testFResultUndefined(armyLogic.setTroopAmount("notknown","asdasasd",amount = troopAmount))
    }

    s"update amount of troop does not work when troop was not found" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val troopAmount = 10
      val notChangedArmy = getFResultDefined(armyLogic.setTroopAmount(uuid,"asdasasd",amount = troopAmount))
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

      val troopAmount = 10

      val changedArmy = getFResultDefined(armyLogic.setTroopAmount(uuid,troopUuid,amount = troopAmount))
      changedArmy.troops.length mustBe 1
      changedArmy.troops(0).amount mustBe 10
      changedArmy.totalCosts mustBe 500
    }

    s"remove troop from army works" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      getFResultDefined(armyLogic.addTroopToArmy(uuid, "Warlord"))
      val updatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Warlord"))
      updatedArmy.troops.length mustBe 2
      updatedArmy.totalCosts mustBe 100

      val troopUuidToRemove = updatedArmy.troops(0).uuid

      val removedTroopArmy = getFResultDefined(armyLogic.removeTroopFromArmy(uuid,troopUuidToRemove))
      removedTroopArmy.troops.length mustBe 1
      removedTroopArmy.troops.exists(_.uuid == troopUuidToRemove) mustBe false
      removedTroopArmy.totalCosts mustBe 50
    }

    s"cannot set name on army which does not exists" in {
      testFResultUndefined(armyLogic.setArmyName("asdasdasd","asdasasasd"))
    }

    s"set name on army works" in {
      val uuid = UUID.randomUUID().toString
      val army = getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      army.name mustBe empty
      val armyWithName = getFResultDefined(armyLogic.setArmyName(uuid,"Hero's of the Horde"))
      armyWithName.name mustBe "Hero's of the Horde"
    }

    s"ability slow on changes move and sprint stat" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val army = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Orc Biker"))
      army.troops.length mustBe 1
      val orcBiker = army.troops(0)
      orcBiker.sprint mustBe 18
      orcBiker.move mustBe 9
    }

    s"ability fast on changes move and sprint stat" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithPowerArmorOrc = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Power Armor Orc"))
      armyWithPowerArmorOrc.troops.length mustBe 1
      val orcWithPowerArmor = armyWithPowerArmorOrc.troops(0)
      orcWithPowerArmor.sprint mustBe 8
      orcWithPowerArmor.move mustBe 4
    }

    s"ability bad shot modifies shoot stat" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val updatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Warlord"))
      updatedArmy.troops.length mustBe 1
      val warLord = updatedArmy.troops(0)
      warLord.shoot mustBe 5
    }

    s"ability good shot modifies shoot stat" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val updatedArmy = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Goblin Herd"))
      updatedArmy.troops.length mustBe 1
      val goblinHerd = updatedArmy.troops(0)
      goblinHerd.shoot mustBe 4
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
