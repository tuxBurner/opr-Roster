package service.logic

import javax.inject.{Inject, Singleton}
import play.api.Logger
import service.models.{EUpgradeRuleType, UpgradeRuleDo, UpgradeRuleOptionDo, UpgradesDao, WeaponDao}

import scala.concurrent.Future

/**
  * Handles the upgrade on a troop
  */
@Singleton
class UpgradeLogic @Inject()(armyLogic: ArmyLogic) {

  /**
    * Logger for the army logic
    */
  val LOGGER = Logger(classOf[UpgradeLogic])


  /**
    * Returns all the possible upgrade options the troop can use
    *
    * @param armyUuid  the uuid of the army the troop belongs to
    * @param troopUuid the uuid of the troop to get the possible options for
    * @return all the possible upgrade options the troop has
    */
  def getPossibleUpdatesForTroop(armyUuid: String, troopUuid: String): Future[Option[TroopPossibleUpgradesDto]] = {

    armyLogic.getArmyAndTroopForUpdate(armyUuid, troopUuid, (army, troop) => {
      val possibleUpgradeDos = UpgradesDao.getUpgradesForFactionAndNames(army.factionName, troop.possibleUpgrades)

      val replacements = possibleUpgradeDos.flatMap(upgradeDo => {
        upgradeDo.rules
          .filter(_.ruleType == EUpgradeRuleType.Replace)
          .map(getReplaceOption(troop, _))
      })
        .flatten

      val upgrades = possibleUpgradeDos.flatMap(upgradeDo => {
        upgradeDo.rules
          .filter(_.ruleType == EUpgradeRuleType.Upgrade)
          .map(getUpgradeOption(_))
      })

      val attachments = possibleUpgradeDos.flatMap(upgradeDo => {
        upgradeDo.rules
          .filter(_.ruleType == EUpgradeRuleType.Upgrade)
          .map(getAttachmentOption(troop, _))
      })
        .flatten

      Some(TroopPossibleUpgradesDto(replacements = replacements,
        upgrades = upgrades,
        attachments = attachments))
    }, _ => None)
  }

  /**
    * Gets the attachment option for the troop when the troop has a weapon which matches for the attachment
    *
    * @param troopDto the troop for which the attachment is
    * @param rule     the rule for the attachment
    * @return [[None]] when the attachment does not fit on any weapon of the troop
    */
  private def getAttachmentOption(troopDto: TroopDto, rule: UpgradeRuleDo): Option[UpgradeAttachmentDto] = {
    val filteredAttachments =
      rule.subjects.exists(subjectWeapon => troopDto.currentWeapons.exists(subjectWeapon.linkedName == _.linkedName))

    if (filteredAttachments) {
      val subject = rule.subjects.head
      Some(UpgradeAttachmentDto(subject = armyLogic.weaponDoToDto(subject),
        amount = rule.amount,
        options = rule.options.map(upgradeOptionDoToDto)))
    } else {
      LOGGER.info(s"Cannot use attachment for subject: ${rule.subjects.map(_.linkedName).mkString} it is not equipped by the troop: ${troopDto.name}  ${troopDto.currentWeapons.map(_.linkedName).mkString}")
      None
    }
  }

  /**
    * Gets the upgrade option for the troop
    *
    * @param rule the rule for the upgrade
    * @return the upgrade and its options
    */
  private def getUpgradeOption(rule: UpgradeRuleDo): UpgradeUpgradeDto = {
    UpgradeUpgradeDto(amount = rule.amount,
      options = rule.options.map(upgradeOptionDoToDto))
  }

  /**
    * Gets the replace option for the troop when the troop has equiped the weapon combination which matches
    *
    * @param troopDto the troop for which the replacement is
    * @param rule     the rule for the attachment
    * @return [[None]] when the replacement does not fit on any weapon of the troop
    */
  private def getReplaceOption(troopDto: TroopDto, rule: UpgradeRuleDo): Option[UpgradeReplaceDto] = {
    // check if the rule can be applied to the current troop
    val filteredWeapons = rule
      .subjects
      .filter(subjectWeapon => troopDto.currentWeapons.exists(troopWeapon => {
        troopWeapon.linkedName == subjectWeapon.linkedName
      }))

    if (filteredWeapons.size != rule.subjects.size) {
      LOGGER.info(s"Cannot use ${rule.ruleType} subjects: ${rule.subjects.map(_.linkedName).mkString} are not in the current weapons of troop: ${troopDto.name} ${troopDto.currentWeapons.map(_.linkedName).mkString}")
      None
    } else {
      val upgradeReplaceDto = UpgradeReplaceDto(subjects = rule.subjects.map(armyLogic.weaponDoToDto),
        amount = rule.amount,
        options = rule.options.map(upgradeOptionDoToDto)
      )
      Some(upgradeReplaceDto)
    }
  }

  /**
    * Sets the upgrades on the given troop and army
    *
    * @param armyUuid  the uuid of the army
    * @param troopUuid the uuid of the troop
    * @return
    */
  def setReplacementOnTroop(armyUuid: String, troopUuid: String, replace: SetReplacementOption): Future[Option[ArmyDto]] = {
    armyLogic.getArmyAndTroopForUpdate(armyUuid,troopUuid,(army,troop) => {
      val troopsWeapon = troop.
        currentWeapons.flatMap(weapon => {
         if(replace.subjects.contains(weapon.linkedName)) {
           None
         } else {
           Some(weapon)
         }
      })

      val newWeapons = replace.replaceWith.weapons
        .map(WeaponDao.findWeaponByLinkedNameAndFactionName(_,army.factionName))
          .flatten
          .map(armyLogic.weaponDoToDto(_))

      val weaponsToSet = troopsWeapon ++ newWeapons
      //Some(troop.copy())
      None
    },(army) => Some(army))
  }

  /**
    * Transforms an [[UpgradeRuleOptionDo]] to an [[UpgradeOptionDto]]
    *
    * @param upgradeOptionDo the upgrade option to transform
    * @return the created [[UpgradeOptionDto]]
    */
  def upgradeOptionDoToDto(upgradeOptionDo: UpgradeRuleOptionDo): UpgradeOptionDto = {
    UpgradeOptionDto(costs = upgradeOptionDo.costs,
      weapons = upgradeOptionDo.weapons.map(armyLogic.weaponDoToDto),
      abilities = upgradeOptionDo.abilities.map(armyLogic.abilityDoToDto),
      items = upgradeOptionDo.items.map(armyLogic.itemDoToDto))
  }
}

/**
  * Represents a replace update the user can replace some stuff with other stuff
  *
  * @param subjects the subjects which are replaced
  * @param amount   how many may be replaced
  * @param options  the options which can be choose for replacement
  */
case class UpgradeReplaceDto(subjects: Set[WeaponDto],
                             amount: Int,
                             options: Set[UpgradeOptionDto])

/**
  * Represents an attachment update the user can replace some stuff with other stuff
  *
  * @param subject the subject for which the attachment can be selected
  * @param amount  how many may be replaced
  * @param options the options which can be choose for replacement
  */
case class UpgradeAttachmentDto(subject: WeaponDto,
                                amount: Int,
                                options: Set[UpgradeOptionDto])

/**
  * Represents a upgrade update the user can replace some stuff with other stuff
  *
  * @param amount  how many may be replaced
  * @param options the options which can be choose for replacement
  */
case class UpgradeUpgradeDto(amount: Int,
                             options: Set[UpgradeOptionDto])


/**
  * Represents an option which can be chosen from
  *
  * @param costs     how much does this option costs
  * @param weapons   what weapons does the option bring
  * @param abilities what abilities does the option bring
  * @param items     what items does the option bring
  */
case class UpgradeOptionDto(costs: Int,
                            weapons: Set[WeaponDto],
                            abilities: Set[AbilityDto],
                            items: Set[ItemDto])

/**
  * Represents all possible upgrades a troop can have currently
  *
  * @param replacements replacements of weapons
  * @param upgrades     upgrades the troop can have
  * @param attachments  attachements the troop can choose from
  */
case class TroopPossibleUpgradesDto(replacements: List[UpgradeReplaceDto],
                                    upgrades: List[UpgradeUpgradeDto],
                                    attachments: List[UpgradeAttachmentDto])

case class SetReplacementOption(subjects: Set[String],
                                replaceWith: SetEquipment)

case class SetEquipment(weapons: Set[String] = Set.empty,
                        items: Set[String] = Set.empty,
                        abilities: Set[String] = Set.empty)
