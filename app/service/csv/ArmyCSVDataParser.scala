package service.csv

import javax.inject.{Inject, Singleton}
import play.api.Configuration

/**
  * Parses the armies to factions and there troops from the armies csv
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class ArmyCSVDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVFactionDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.FACTIOM_HEADER, CSVHeaders.NAME_HEADER, CSVHeaders.SIZE_HEADER, CSVHeaders.QUALITY_HEADER, CSVHeaders.DEFENSE_HEADER, CSVHeaders.COST_HEADER)

  override def getFileName(): String = "armies"

  override def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String], Int)]): Set[CSVFactionDto] = {


    val troops =
      dataFromCsvFile
        .filter(info => filterRequiredLines(requiredHeaders, info))
        .flatMap(info => {

          val faction = info._1.get(CSVHeaders.FACTIOM_HEADER)
          val troopName = info._1.get(CSVHeaders.NAME_HEADER)
          val size = readCsvLineToInt(CSVHeaders.SIZE_HEADER, info)
          val quality = readCsvLineToInt(CSVHeaders.QUALITY_HEADER, info)
          val defence = readCsvLineToInt(CSVHeaders.DEFENSE_HEADER, info)
          val defaultEquipment = readCsvLineToSet(CSVHeaders.EQUIPMENT_HEADER, info)
          val defaultAbilities = readCsvLineToSet(CSVHeaders.ABILITIES_HEADER, info)
          val upgrades = readCsvLineToSet(CSVHeaders.UPGRADES_HEADER, info)
          val costs = readCsvLineToInt(CSVHeaders.COST_HEADER, info)

          if (costs.isEmpty || defence.isEmpty || quality.isEmpty || size.isEmpty) {
            None
          } else {

            val troop = CSVTroopDto(
              name = troopName.get,
              factionName = faction.get,
              size = size.get,
              quality = quality.get,
              defense = defence.get,
              defaultEquipment = defaultEquipment,
              defaultAbilities = defaultAbilities,
              upgrades = upgrades,
              costs = costs.get
            )

            Some(troop)
          }
        })

    troops
      .groupBy(_.factionName)
      .values
      .map((troops) => CSVFactionDto(troops(0).factionName, troops.toSet))
      .toSet
  }

}

/**
  * Represents a faction from the armies.csv
  *
  * @param name   name of the faction
  * @param troops troops which are in the faction
  */
case class CSVFactionDto(name: String,
                         troops: Set[CSVTroopDto])

/**
  * Represents a troop from the armies.csv
  *
  * @param name             the name of the troop
  * @param factionName      the name of the faction the troop belongs to
  * @param size             the size of the troop
  * @param quality          the quality of the troop
  * @param defense          the defence of the troop
  * @param defaultEquipment the default equipment the troop has
  * @param defaultAbilities the default abilities the troop has
  * @param upgrades         the upgrades the troop can have
  * @param costs            the costs of the troop
  */
case class CSVTroopDto(name: String,
                       factionName: String,
                       size: Int,
                       quality: Int,
                       defense: Int,
                       defaultEquipment: Set[String],
                       defaultAbilities: Set[String],
                       upgrades: Set[String],
                       costs: Int)

