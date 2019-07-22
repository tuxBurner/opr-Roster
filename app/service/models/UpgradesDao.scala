package service.models

import play.api.Logger
import service.csv.{CSVUpgradeDto, CSVUpgradeWithDTO}

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
    * @param csvUpgrade the upgrade information from the csv
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
      val ruleType = EUpgradeRuleType.valueOf(csvRule.ruleType)

      val subjects = collectSubjects(csvRule.subjects, factionDo, csvUpgrade, ruleType)

      val upgradeOptions = csvRule.options.map(csvUpgradeOption => {
        val weapons = getWithWeapons(csvUpgradeOption.upgradeWith, factionDo, csvUpgrade, ruleType)
        val abilities = getWithAbilities(csvUpgradeOption.upgradeWith, factionDo, csvUpgrade, ruleType)
        val items = getWithItems(csvUpgradeOption.upgradeWith, factionDo, csvUpgrade, ruleType)

        val uniqueId = s"${csvUpgrade.factionName}_" +
          s"${csvUpgrade.name}_" +
          s"${ruleType.name}_" +
          s"${subjects.map(_.linkedName).mkString("_")}" +
          s"_${csvRule.amount}_" +
          s"${weapons.map(_.linkedName).mkString("_")}" +
          s"${abilities.map(ability => ability.ability.name + ability.modifyValue.getOrElse("")).mkString("_")}" +
          s"${items.map(_.name).mkString("_")}" +
          s"_${csvUpgradeOption.costs}"

        UpgradeRuleOptionDo(uuid = uniqueId,
          costs = csvUpgradeOption.costs,
          weapons = weapons,
          abilities = abilities,
          items = items)
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
    * Gets all the upgrades for the given faction an names
    *
    * @param factionName  the name of the faction
    * @param upgradeNames the names of the upgrades to get
    * @return [[List]] of upgrades which matches the faction and the names
    */
  def getUpgradesForFactionAndNames(factionName: String, upgradeNames: Set[String]): List[UpgradeDo] = {
    upgrades
      .filter(upgrade => upgrade.faction.name == factionName && upgradeNames.contains(upgrade.name))
      .toList
      .sortBy(_.name)
  }

  /**
    * Gets the upgrade for the given faction and name
    *
    * @param factionName the name of the faction
    * @param upgradeName the name of the upgrade
    * @return [[None]] when the upgrade was not found  [[Some]] when the upgrade was found
    */
  def getUpgradeForFactionByName(factionName: String, upgradeName: String): Option[UpgradeDo] = {
    if (upgradeName.isEmpty) {
      None
    } else {
      upgrades
        .find(upgrade => upgrade.faction.name == factionName && upgradeName == upgrade.name)
        .map(Some(_))
        .getOrElse({
          LOGGER.error(s"Could not find upgrade: $upgradeName for faction: $factionName")
          None
        })
    }
  }

  /**
    * Removes all the [[UpgradeDo]]
    */
  def deletAll(): Unit = {
    upgrades.clear()
  }

  /**
    * Filters the upgrade with by the given type
    *
    * @param upgradeWith the upgrade with to filter
    * @param filterType  the type to filter with
    * @return the filtered upgrade with names
    */
  private def filterUpgradeWithByTypeAndGetNames(upgradeWith: Set[CSVUpgradeWithDTO], filterType: EUpgradeWithType): Set[String] = {
    upgradeWith
      .filter(_.withType == filterType.csvKey)
      .map(_.withName)
  }


  /**
    * Gets all with weapons
    *
    * @param upgradeWith set of the with options for an upgrade
    * @param factionDo   the faction for the upgrade
    * @param csvUpgrade  the upgrade itself
    * @param ruleType    the current rule type
    * @return set of [[WeaponDo]]
    */
  private def getWithWeapons(upgradeWith: Set[CSVUpgradeWithDTO], factionDo: FactionDo, csvUpgrade: CSVUpgradeDto, ruleType: EUpgradeRuleType): Set[WeaponDo] = {
    // get all the weapons which are used by the option
    val upgradeWeapons = filterUpgradeWithByTypeAndGetNames(upgradeWith, EUpgradeWithType.WEAPON)
    WeaponDao.findWeaponsForCsv(factionDo.name, upgradeWeapons, weaponName => {
      LOGGER.error(s"Cannot find weapon: $weaponName for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
    })
  }

  /**
    * Gets all with abilities
    *
    * @param upgradeWith set of the with options for an upgrade
    * @param factionDo   the faction for the upgrade
    * @param csvUpgrade  the upgrade itself
    * @param ruleType    the current rule type
    * @return set of [[AbilityWithModifyValueDo]]
    */
  private def getWithAbilities(upgradeWith: Set[CSVUpgradeWithDTO], factionDo: FactionDo, csvUpgrade: CSVUpgradeDto, ruleType: EUpgradeRuleType): Set[AbilityWithModifyValueDo] = {
    val upgradeAbilitiesName = filterUpgradeWithByTypeAndGetNames(upgradeWith, EUpgradeWithType.ABILITY)
    AbilitiesDao.findAbilitiesForCsv(upgradeAbilitiesName, ability => {
      LOGGER.error(s"Cannot find ability: $ability for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
    })
  }

  /**
    * Gets all with items
    *
    * @param upgradeWith set of the with options for an upgrade
    * @param factionDo   the faction for the upgrade
    * @param csvUpgrade  the upgrade itself
    * @param ruleType    the current rule type
    * @return set of [[ItemDo]]
    */
  private def getWithItems(upgradeWith: Set[CSVUpgradeWithDTO], factionDo: FactionDo, csvUpgrade: CSVUpgradeDto, ruleType: EUpgradeRuleType): Set[ItemDo] = {
    val upgradeItemNames = filterUpgradeWithByTypeAndGetNames(upgradeWith, EUpgradeWithType.ITEM)
    ItemDao.findItemsForCsv(upgradeItemNames, itemName => {
      LOGGER.error(s"Cannot find item: $itemName for upgrade: ${csvUpgrade.name} for rule: $ruleType faction: ${factionDo.name}")
    })
  }

  /**
    * Gets all subjects
    *
    * @param csvSubjects the csv subjects
    * @param factionDo   the faction for the upgrade
    * @param csvUpgrade  the upgrade itself
    * @param ruleType    the current rule type
    * @return set of [[WeaponDo]]
    */
  private def collectSubjects(csvSubjects: Set[String], factionDo: FactionDo, csvUpgrade: CSVUpgradeDto, ruleType: EUpgradeRuleType): Set[WeaponDo] = {
    // collect the subject weapons
    csvSubjects.flatMap(subject => {
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
case class UpgradeRuleDo(ruleType: EUpgradeRuleType,
                         subjects: Set[WeaponDo],
                         amount: Int,
                         options: Set[UpgradeRuleOptionDo])

/**
  * Options of an upgrade rule
  *
  * @param uuid      the unique id of the rule
  * @param costs     how much does the rule cost
  * @param weapons   what weapon come with the option
  * @param abilities what abilities come with the option
  * @param items     what items come with the option
  */
case class UpgradeRuleOptionDo(uuid: String,
                               costs: Int,
                               weapons: Set[WeaponDo],
                               abilities: Set[AbilityWithModifyValueDo],
                               items: Set[ItemDo])
