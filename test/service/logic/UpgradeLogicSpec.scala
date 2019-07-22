package service.logic

import java.util.UUID


/**
  * Tests for the  [[UpgradeLogic]]
  */
class UpgradeLogicSpec extends BasicOprLogicSpec {

  "UpgradeLogic" should {

    "return empty when army is not present" in {
      val uuid = UUID.randomUUID().toString
      testFResultUndefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, "asdasd"))
    }

    "return empty when troop is not present" in {
      val uuid = UUID.randomUUID().toString
      val factionKnown = getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      factionKnown.factionName mustBe orcMarauderFaction
      testFResultUndefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, "TroopNotKnown"))
    }

    "return something when the troop is known" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid, orcWarlordName))
      val troopUuid = armyWithTroop.troops.head.uuid

      getFResultDefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, troopUuid))
    }

    "replace the pistol on the orc with carabine" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Orc"))

      val troopUuid = armyWithTroop.troops.head.uuid

      // check the default options
      val upgrades = getFResultDefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, troopUuid))
      upgrades.replacements.length mustBe 2
      upgrades.attachments mustBe empty
      upgrades.upgrades.length mustBe 1
      upgrades.upgrades.exists(upgrade =>  upgrade.options.exists(_.uuid == "Orc Marauders_O_Upgrade__1_Heavy Armor_5")) mustBe true

      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_A_Replace_Pistol_0_Twin Carbine_10")) mustBe true
      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_A_Replace_Pistol_0_Carbine_5")) mustBe true
      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_O_Replace_Pistol_0_Heavy Machinegun_15")) mustBe true

      //upgradeLogic.setReplacementOnTroop(uuid,troopUuid)
    }
  }

}
