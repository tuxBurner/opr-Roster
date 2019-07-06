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
    csvUpgrade.rules.flatMap(csvRule => {
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

      csvRule.options.map(csvUpgradeOption => {

        // get all the weapons which are used by the option
        val upgradeWeapons = csvUpgradeOption
          .upgradeWith
          .filter(_.withType == "W")
          .map(_.withName)

        val weapons = WeaponDao.findWeaponsForCsv(factionDo.name, upgradeWeapons, (weaponName) => {
          LOGGER.error(s"Cannot find weapon: $weaponName for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
        })


        val upgardeAbilitiesName = csvUpgradeOption
          .upgradeWith
          .filter(_.withType == "A")
          .map(_.withName)
        val abilities = AbilitiesDao.findAbilitiesForCsv(upgardeAbilitiesName, (ability) => {
          LOGGER.error(s"Cannot find ability: $ability for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
        })


        UpgradeOptionDo(costs = csvUpgradeOption.costs,
          weapons = weapons,
          abilities = abilities)

      })

      /*Some(UpgradeRuleDo(ruleType = ruleType,
        subjects = subjects,
        amount = csvRule.amount))*/
      None
    })
  }

  /**
    * Removes all the [[UpgradeDo]]
    */
  def deletAll(): Unit = {
    upgrades.clear()
  }
}


case class UpgradeDo(name: String,
                     faction: FactionDo,
                     rules: Set[UpgradeRuleDo])

case class UpgradeRuleDo(ruleType: UpgradeRuleType,
                         subjects: Set[WeaponDo],
                         amount: Int,
                         options: Set[UpgradeOptionDo])

case class UpgradeOptionDo(costs: Int,
                           weapons: Set[WeaponDo],
                           abilities: Set[AbilityWithModifyValueDo])
