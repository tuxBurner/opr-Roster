package service.models

import play.api.Logger
import service.csv.CSVUpgradeDto

import scala.collection.mutable.ListBuffer

/**
  * Dao which handles all the upgrades
  */
object UpgradesDao {

  /**
    * Logger
    */
  private val LOGGER = Logger("UpgradesDao")


  /**
    * Holds all the [[UpgradeDo]]
    */
  val upgrades: ListBuffer[UpgradeDo] = ListBuffer()

  /**
    * Adds the given csv upgrade to the database
    *
    * @param csvUpgrade
    */
  def addUpgradeFromCsv(csvUpgrade: CSVUpgradeDto): Unit = {

    // check if the faction exists
    val factionOption = FactionsDao.findFactionByName(csvUpgrade.factionName)
    if (factionOption.isEmpty) {
      LOGGER.error(s"Cannot add upgrade: ${csvUpgrade.name} the faction: ${csvUpgrade.factionName} does not exists")
      return
    }

    val factionDo = factionOption.get

    // map the rules
    val upgradeRules = csvUpgrade.rules.flatMap(csvRule => {
      val ruleType = UpgradeRuleType.valueOf(csvRule.ruleType)

      // collect the subject weapons
      val subjects = csvRule.subjects.flatMap(subject => {
        if (subject.isEmpty) {
          None
        } else {
          val weaponDo = WeaponDao.findWeaponByLinkedNameAndFactionName(subject.trim, factionDo.name)
          if (weaponDo.isEmpty) {
            LOGGER.error(s"Cannot find weapon: $subject for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
          }
          weaponDo
        }
      })

      val upgradeOptions = csvRule.options.map(csvUpgradeOption => {

        // get all the weapons which are used by the option
        val upgradeWeapons = csvUpgradeOption
          .upgradeWith
          .filter(_.withType == "W")
          .map(_.withName)
        val weapons = WeaponDao.findWeaponsForCsv(factionDo.name, upgradeWeapons, (weaponName) => {
          LOGGER.error(s"Cannot find weapon: $weaponName for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
        })


        val upgradeAbilitiesName = csvUpgradeOption
          .upgradeWith
          .filter(_.withType == "A")
          .map(_.withName)
        val abilities = AbilitiesDao.findAbilitiesForCsv(upgradeAbilitiesName, (ability) => {
          LOGGER.error(s"Cannot find ability: $ability for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
        })

        val upgradeItemNames = csvUpgradeOption
          .upgradeWith
          .filter(_.withType == "I")
          .map(_.withName)
        val items = ItemDao.findItemsForCsv(upgradeItemNames, (itemName) => {
          LOGGER.error(s"Cannot find item: $itemName for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
        })


        UpgradeRuleOptionDo(costs = csvUpgradeOption.costs,
          weapons = weapons,
          abilities = abilities)

      })

      Some(UpgradeRuleDo(ruleType = ruleType,
        subjects = subjects,
        amount = csvRule.amount,
        options = upgradeOptions))
    })

    val upgrade = UpgradeDo(name = csvUpgrade.name,
      faction = factionDo,
      rules = upgradeRules)

    upgrades += upgrade
  }

  /**
    * Removes all the [[UpgradeDo]]
    */
  def deletAll(): Unit = {
    upgrades.clear()
  }
}


/**
  * Represents an upgrade
  *
  * @param name    the name of the upgrade
  * @param faction the faction the upgrade belongs to
  * @param rules   the upgrade rules
  */
case class UpgradeDo(name: String,
                     faction: FactionDo,
                     rules: Set[UpgradeRuleDo])

/**
  * Rule of an upgrade
  *
  * @param ruleType the type of the rule
  * @param subjects on what the rule applies
  * @param amount   ho often cann the rule be applied
  * @param options  the options the rul provides
  */
case class UpgradeRuleDo(ruleType: UpgradeRuleType,
                         subjects: Set[WeaponDo],
                         amount: Int,
                         options: Set[UpgradeRuleOptionDo])

/**
  * Options of an upgrade rule
  *
  * @param costs     how much does the rule cost
  * @param weapons   what weapon come with the option
  * @param abilities what abilities come with the option
  */
case class UpgradeRuleOptionDo(costs: Int,
                               weapons: Set[WeaponDo],
                               abilities: Set[AbilityWithModifyValueDo])
