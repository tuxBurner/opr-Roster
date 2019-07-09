package service.logic

import org.scalatest.AsyncFlatSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import service.DataInitializer


/**
  * Tests for the army logic
  */
class ArmyLogicSpec extends AsyncFlatSpec {


  val fakeApplication: Application = new GuiceApplicationBuilder().build()

  val armyLogic: ArmyLogic = fakeApplication.injector.instanceOf(classOf[ArmyLogic])
  val dataInitalizer: DataInitializer = fakeApplication.injector.instanceOf(classOf[DataInitializer])

  dataInitalizer.initData()

  val orcMarauderFaction = "Orc Marauders"

  behavior of "ArmyLogic"

  it should "return empty when army is not present" in {
    val armyNotKnown = armyLogic.getArmy("asdasasd")
    armyNotKnown.map(armyOption => {
      assert(armyOption.isEmpty)
    })
  }

  it should "return empty when the faction name is not set correctly" in {
    val factionNotKnown = armyLogic.addNewArmy("FactionNotKnow")
    factionNotKnown.map(armyOption => {
      assert(armyOption.isEmpty)
    })
  }

  it should "return an army of Orc Marauders when adding an army" in {
    val factionKnown = armyLogic.addNewArmy(orcMarauderFaction)
    factionKnown.map(armyOption => {
      assert(armyOption.isDefined)
      assert(armyOption.get.factionName == orcMarauderFaction)
    })
  }

  it should "return no troops when troop name not known in faction" in {
    val factionKnown = armyLogic.addNewArmy(orcMarauderFaction)
    factionKnown.map(armyOption => {
      assert(armyOption.isDefined)

      val armyWithNoTroops = armyLogic.addTroopToArmy(armyOption.get.uuid,"ToopNotKnow")
      armyWithNoTroops.map(army => {
        assert(army.isDefined)
      }).flatten

    })
  }


}
