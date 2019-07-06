package service.models

import play.api.Logger
import service.csv.CSVWeaponDto

import scala.collection.mutable.ListBuffer

/**
  * Handles the weapon do
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object WeaponDao {

  /**
    * Stores all the weapons
    */
  val weapons: ListBuffer[WeaponDo] = ListBuffer()

  private val LOGGER = Logger("WeaponDao")

  /**
    * Adds a weapon from the csv
    *
    * @param csvWeaponDto the csv informations for the weapon
    */
  def addWeaponFromCSV(csvWeaponDto: CSVWeaponDto): Unit = {
    val alreadyExists = weapons
      .exists(weaponDo => weaponDo.name == csvWeaponDto.name &&
        weaponDo.linkedName == csvWeaponDto.linkedName &&
        weaponDo.faction.name == csvWeaponDto.factionName)

    if (alreadyExists) {
      LOGGER.error(s"Cannot add weapon: ${csvWeaponDto.linkedName}/${csvWeaponDto.name} to faction: ${csvWeaponDto.factionName} it already exists")
    } else {
      val factionDo = FactionsDao.findFactionByName(csvWeaponDto.factionName)
      if (factionDo.isEmpty) {
        LOGGER.error(s"Cannot add weapon: ${csvWeaponDto.linkedName}/${csvWeaponDto.name} to faction: ${csvWeaponDto.factionName} faction not found")
      } else {
        LOGGER.info(s"Adding new weapon: ${csvWeaponDto.linkedName}/${csvWeaponDto.name} to faction: ${csvWeaponDto.factionName}")

        val abilities = AbilitiesDao.findAbilitiesForCsv(csvWeaponDto.abilities, (abilityName) => {
          LOGGER.error(s"Cannot find ability: ${abilityName} for weapon: ${csvWeaponDto.linkedName}/${csvWeaponDto.name} faction: ${csvWeaponDto.factionName}")
        })

        val weaponDo = WeaponDo(name = csvWeaponDto.name,
          linkedName = csvWeaponDto.linkedName,
          faction = factionDo.get,
          range = csvWeaponDto.range,
          attacks = csvWeaponDto.attacks,
          armorPiercing = csvWeaponDto.armorPiercing,
          abilities = abilities)

        weapons += weaponDo
      }
    }
  }

  /**
    * Gets all the weapons which match the linked name and the faction name
    *
    * @param linkedName  the linked name of the weapon
    * @param factionName the faction name
    * @return [[Set]] of [[WeaponDo]]
    */
  def findWeaponByLinkedNameAndFactionName(linkedName: String, factionName: String): Set[WeaponDo] = {
    weapons
      .filter(weapon => weapon.linkedName == linkedName && weapon.faction.name == factionName)
      .toSet
  }

  /**
    * Gets the weapons from the csv string
    *
    * @param factionName the name of the faction to get the weapons for
    * @param weaponsFromCsv the strings from the csv
    * @param errorLog       the logging callback
    * @return [[Set]] of [[WeaponDo]]
    */
  def findWeaponsForCsv(factionName:String,weaponsFromCsv: Set[String], errorLog: (String) => Unit): Set[WeaponDo] = {
    weaponsFromCsv.flatMap(weaponName => {
      if (weaponName.isEmpty) {
        None
      } else {
        val weaponDo = WeaponDao.findWeaponByLinkedNameAndFactionName(weaponName.trim, factionName)
        if (weaponDo.isEmpty) {
          errorLog(weaponName)
        }
        weaponDo
      }
    })
  }


  /**
    * Deletes all weapons
    */
  def deletAll(): Unit = {
    weapons.clear()
  }
}

/**
  * Represents a weapon
  *
  * @param name          the name of the weapon
  * @param linkedName    the linked name when it is a weapon combo
  * @param faction       the faction the weapon belongs to
  * @param range         the range the weapon has
  * @param attacks       how many attacks has the weapon
  * @param armorPiercing the armor piercing the weapon has
  * @param abilities     abilities the weapon has
  */
case class WeaponDo(name: String,
                    linkedName: String,
                    faction: FactionDo,
                    range: Int,
                    attacks: Int,
                    armorPiercing: Int,
                    abilities: Set[AbilityWithModifyValueDo])


