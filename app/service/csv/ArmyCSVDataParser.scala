package service.csv

import javax.inject.{Inject, Singleton}
import org.apache.commons.lang3.StringUtils
import play.api.Configuration

/**
  * @author Sebastian Hardt (s.hardt@micromata.de)
  *         Date: 02.07.19
  *         Time: 19:00
  */
@Singleton
class ArmyCSVDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVFactionDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.FACTIOM_HEADER, CSVHeaders.NAME_HEADER, CSVHeaders.SIZE_HEADER, CSVHeaders.QUALITY_HEADER, CSVHeaders.DEFENCE_HEADER, CSVHeaders.COST_HEADER)

  override def getFileName(): String = "armies"

  override def readCsvDataInternal(dataFromCsvFile: List[Map[String, String]]): Set[CSVFactionDto] = {


    val troops =
      dataFromCsvFile
        .zipWithIndex
        .filter(info => filterRequiredLines(requiredHeaders, info))
        .flatMap(info => {

          val faction = info._1.get(CSVHeaders.FACTIOM_HEADER)
          val troopName = info._1.get(CSVHeaders.NAME_HEADER)
          val size = readCsvLineToInt(CSVHeaders.SIZE_HEADER, info)
          val quality = readCsvLineToInt(CSVHeaders.QUALITY_HEADER, info)
          val defence = readCsvLineToInt(CSVHeaders.DEFENCE_HEADER, info)
          val defaultEquipment = readCsvLineToSet(CSVHeaders.EQUIPMENT_HEADER, info)
          val defaultAbilities = readCsvLineToSet(CSVHeaders.ABILITIES_HEADER, info)
          val upgrades = readCsvLineToSet(CSVHeaders.UPGRADES_HEADER, info)
          val costs = readCsvLineToInt(CSVHeaders.COST_HEADER, info)

          if(costs.isEmpty || defence.isEmpty || quality.isEmpty || size.isEmpty) {
            None
          } else {

            val troop = CSVTroopDto(
              name = troopName.get,
              factionName = faction.get,
              size = size.get,
              quality = quality.get,
              defence = defence.get,
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

  private def filterRequiredLines(requiredColumns: Set[String], csvInfo: (Map[String, String], Int)): Boolean = {
    requiredColumns.forall(colum => {
      val colVal = csvInfo._1.get(colum)
      if (colVal.isEmpty || StringUtils.isBlank(colVal.get)) {
        LOGGER.error(s"Line ${csvInfo._1.values.mkString} in armies.csv: ${csvInfo._2} has no value at column ${colum} set")
        false
      } else {
        true
      }
    })
  }

  private def readCsvLineToInt(csvHeader: String, csvInfo: (Map[String, String], Int)): Option[Int] = {
    val stringVal = csvInfo._1.get(csvHeader)
    if (stringVal.isEmpty) {
      return None
    }

    try {
      Some(stringVal.get.toInt)
    } catch {
      case e: Exception => {
        LOGGER.error(s"Line in armies.csv: ${csvInfo._2} from colum ${csvHeader} value: ${stringVal.get} is not a number ")
        None
      }
    }
  }

  private def readCsvLineToSet(csvHeader: String, csvInfo: (Map[String, String], Int)): Set[String] = {
    csvInfo._1.get(csvHeader)
      .map(_.trim.split(",").toSet)
      .getOrElse({
        Set.empty
      })
  }

}

case class CSVFactionDto(name: String,
                         troops: Set[CSVTroopDto])

case class CSVTroopDto(name: String,
                       factionName: String,
                       size: Int,
                       quality: Int,
                       defence: Int,
                       defaultEquipment: Set[String],
                       defaultAbilities: Set[String],
                       upgrades: Set[String],
                       costs: Int)
