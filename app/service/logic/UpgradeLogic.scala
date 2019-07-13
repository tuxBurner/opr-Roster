package service.logic

import javax.inject.{Inject, Singleton}
import service.models.{EUpgradeRuleType, UpgradeRuleDo, UpgradesDao}

import scala.concurrent.Future

/**
  * Handles the upgrade on a troop
  */
@Singleton
class UpgradeLogic @Inject()(armyLogic: ArmyLogic) {


  /**
    * Returns all the possible upgrade options the troop can use
    *
    * @param armyUuid  the uuid of the army the troop belongs to
    * @param troopUuid the uuid of the troop to get the possible options for
    * @return [[List]] of all possible options the troop can select as an upgrade
    */
  def getPossibleUpdatesForTroop(armyUuid: String, troopUuid: String): Any = {
    armyLogic.getArmyAndTroopForUpdate(armyUuid, troopUuid, (army, troop) => {
      val possibleUpgradeDos = UpgradesDao.getUpgradesForFactionAndNames(army.factionName, troop.possibleUpgrades)
      possibleUpgradeDos.map(upgradeDo => {
        upgradeDo.rules
          .map(ruleDo => {
            ruleDo.ruleType match {
              case EUpgradeRuleType.Replace =>
            }
          })
      })
      None
    })
  }

  def getReplaceUpgradeOption(troopDto: TroopDto, rule: UpgradeRuleDo): Unit = {
    // check if the rule can be applied to the current troop
    ???
  }

  /**
    * Sets the upgrades on the given troop and army
    *
    * @param armyUuid  the uuid of the army
    * @param troopUuid the uuid of the troop
    * @return
    */
  def setUpgradesOnTroop(armyUuid: String, troopUuid: String): Future[Option[ArmyDto]] = {
    armyLogic.getArmyAndTroopForUpdate(armyUuid, troopUuid, (army, troop) => {

      Some(army)
    })
  }
}

case class UgradeOptionDto(upgradeName: String)
