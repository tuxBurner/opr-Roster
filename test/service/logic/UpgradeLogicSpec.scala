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
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Orc"))

      val troopUuid = armyWithTroop.troops.head.uuid

      // check the default options
      val upgrades = getFResultDefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, troopUuid))
      upgrades.replacements.length mustBe 3
      upgrades.attachments mustBe empty
      upgrades.upgrades.length mustBe 1
      upgrades.upgrades.exists(upgrade => upgrade.options.exists(_.uuid == "Orc Marauders_O_Upgrade__1_Heavy Armor_5")) mustBe true

      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_A_Replace_Pistol_0_Twin Carbine_10")) mustBe true
      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_A_Replace_Pistol_0_Carbine_5")) mustBe true
      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_A_Replace_CCW2_0_Energy Sword_10")) mustBe true
      upgrades.replacements.exists(replacement => replacement.options.exists(_.uuid == "Orc Marauders_O_Replace_Pistol_0_Heavy Machinegun_15")) mustBe true
    }

    "cannot set unknown replacement" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Orc"))

      val troopUuid = armyWithTroop.troops.head.uuid

      val replacementUUId = "Not Known"
      val updatedArmy = getFResultDefined(upgradeLogic.setReplacementOnTroop(uuid,troopUuid, replacementUUId))


      val notChangedTroop = updatedArmy.troops.head
      notChangedTroop.currentWeapons.size mustBe 2
      notChangedTroop.currentWeapons.exists(_.linkedName == "Pistol") mustBe true
      notChangedTroop.currentWeapons.exists(_.linkedName == "CCW2") mustBe true
    }

    "set replacement Orc Marauders_A_Replace_Pistol_0_Carbine_5" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Orc"))

      val troopUuid = armyWithTroop.troops.head.uuid

      val replacementUUId = "Orc Marauders_A_Replace_Pistol_0_Carbine_5"
      val updatedArmy = getFResultDefined(upgradeLogic.setReplacementOnTroop(uuid,troopUuid, replacementUUId))

      val changedTroop = updatedArmy.troops.head
      changedTroop.currentWeapons.size mustBe 2
      changedTroop.currentWeapons.exists(_.linkedName == "Carbine") mustBe true
      changedTroop.currentWeapons.exists(_.linkedName == "CCW2") mustBe true

      // costs must have changed
      changedTroop.costs mustBe 20

      // check if the selected replacement is set at the troop
      changedTroop.selectedReplacements.contains(replacementUUId) mustBe true

      // check if we have attachments now
      val upgrades = getFResultDefined(upgradeLogic.getPossibleUpdatesForTroop(uuid, troopUuid))
      upgrades.attachments.length mustBe 1

      // replacement must be still 2 because we still have the options
      upgrades.replacements.length mustBe 2
    }

    "replace the ccw and the pistol on an orc" in {
      val uuid = UUID.randomUUID().toString
      getFResultDefined(armyLogic.addNewArmy(uuid, orcMarauderFaction))
      val armyWithTroop = getFResultDefined(armyLogic.addTroopToArmy(uuid, "Orc"))

      val troopUuid = armyWithTroop.troops.head.uuid

      val replacementPistolUUId = "Orc Marauders_A_Replace_Pistol_0_Carbine_5"
      getFResultDefined(upgradeLogic.setReplacementOnTroop(uuid,troopUuid, replacementPistolUUId))

      val replacementCCWUuid = "Orc Marauders_A_Replace_CCW2_0_Energy Sword_10"
      val updatedArmy = getFResultDefined(upgradeLogic.setReplacementOnTroop(uuid,troopUuid, replacementCCWUuid))

      val changedTroop = updatedArmy.troops.head
      changedTroop.selectedReplacements.length mustBe 2
      changedTroop.selectedReplacements.contains(replacementPistolUUId) mustBe true
      changedTroop.selectedReplacements.contains(replacementCCWUuid) mustBe true

      changedTroop.currentWeapons.exists(_.linkedName == "Carbine") mustBe true
      changedTroop.currentWeapons.exists(_.linkedName == "Energy Sword") mustBe true

      // costs must have changed
      changedTroop.costs mustBe 30


    }

    


  }

}
