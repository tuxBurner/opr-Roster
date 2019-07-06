package service.models

import java.util.regex.Pattern

import play.api.Logger
import service.csv.CSVTroopDto

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

/**
  * Handles the troop dos
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object TroopDao {

  /**
    * Stores all the troops
    */
  val troops: ListBuffer[TroopDo] = ListBuffer()

  /**
    * Deletes all the troops
    */
  def deleteAll(): Unit = {
    troops.clear()
  }

  /**
    * All the factions
    */
  private val LOGGER = Logger("TroopDao")

  /**
    * Finds the troop by its name and faction
    *
    * @param troopName   the name of the troop
    * @param factionName the name of the faction
    * @return the [[TroopDo]] or [[None]]
    */
  def getTroopByNameAndFactionName(troopName: String, factionName: String): Option[TroopDo] = {
    troops
      .find(troop => troop.name == troopName && troop.faction.name == factionName)
  }

  def addTroopFromCsvDto(csvTroop: CSVTroopDto, faction: FactionDo): Unit = {

    // check if the troop already exists
    val troopCheck = getTroopByNameAndFactionName(csvTroop.name, csvTroop.factionName)
    if (troopCheck.isDefined) {
      LOGGER.error(s"A troop with the name ${csvTroop.name} in faction: ${csvTroop.factionName} already exists")
    } else {

      LOGGER.info(s"Adding troop: ${csvTroop.name} to faction: ${csvTroop.factionName}")

      val abilities = AbilitiesDao.findAbilitiesForCsv(csvTroop.defaultAbilities,(abilityName) => {
        LOGGER.error(s"Cannot find ability: ${abilityName} for troop: ${csvTroop.name} in faction: ${csvTroop.factionName}")
      })

      val weapons = csvTroop.defaultEquipment
        .flatMap(weaponName => {
          val weapons = WeaponDao.findWeaponByLinkedNameAndFactionName(weaponName, faction.name)
          if (weapons.isEmpty) {
            LOGGER.error(s"Could not find weapon: $weaponName for troop: ${csvTroop.name} in faction: ${csvTroop.factionName}")
          }
          weapons
        })

      val newTroop = TroopDo(name = csvTroop.name,
        faction = faction,
        size = csvTroop.size,
        quality = csvTroop.quality,
        costs = csvTroop.costs,
        defaultWeapons = Set.empty,
        defaultAbilities = abilities
      )
    }
  }

}


case class TroopDo(name: String,
                   faction: FactionDo,
                   size: Int,
                   quality: Int,
                   costs: Int,
                   defaultWeapons: Set[WeaponDo],
                   defaultAbilities: Set[AbilityWithModifyValueDo])
