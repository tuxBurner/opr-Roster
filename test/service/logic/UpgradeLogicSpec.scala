package service.logic

import java.util.UUID


/**
  * Tests for the  [[UpgradeLogic]]
  */
class UpgradeLogicSpec extends BasicOprLogicSpec {

  "UpgradeLogic" should {

    "return empty when army is not present" in {
      val uuid = UUID.randomUUID().toString
      testFResultUndefined(upgradeLogic.getPossibleUpdatesForTroop(uuid,"asdasd"))
    }

    "return empty when troop is not present" in {
      val uuid = UUID.randomUUID().toString
      val factionKnown = getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      factionKnown.factionName mustBe orcMarauderFaction
      testFResultUndefined(upgradeLogic.getPossibleUpdatesForTroop(uuid,"TroopNotKnown"))
    }

    "return something when the troop is known" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid,orcWarlordName))
      val troopUuid = armyWithTroop.troops(0).uuid

      val upgrades = getFResultDefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, troopUuid))
    }
  }

}
