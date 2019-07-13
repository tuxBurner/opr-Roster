package service.csv

import javax.inject.{Inject, Singleton}
import play.api.Configuration

/**
  * Parses the abilities from the csv and does some validation on it
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class AbilitiesCsvDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVAbilityDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.NAME_HEADER)

  override def getFileName(): String = "abilities"

  override def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String], Int)]): Set[CSVAbilityDto] = {
    dataFromCsvFile
      .filter(info => filterRequiredLines(requiredHeaders, info))
      .flatMap(info => {
        val name = info._1(CSVHeaders.NAME_HEADER)
        val modifier = info._1(CSVHeaders.MODIFIER_HEADER).toUpperCase == "X"
        val shoot = readCsvLineToIntWithDefault(CSVHeaders.SHOOT_HEADER, info)
        val move = readCsvLineToIntWithDefault(CSVHeaders.MOVE_HEADER, info)
        val sprint = readCsvLineToIntWithDefault(CSVHeaders.SPRINT_HEADER, info)
        Some(CSVAbilityDto(name = name,
          modifier = modifier,
          shoot = shoot,
          move = move,
          sprint = sprint))
      }).toSet
  }
}

/**
  * Represents an ability from the csv
  *
  * @param name     the name of the ability
  * @param modifier if the ability has a modifier
  * @param shoot    when not 0 the shoot quality of the troop changes
  * @param move     when not 0 the move of the troop changes
  * @param sprint   when not 0 the sprint of the troop changes  *
  */
case class CSVAbilityDto(name: String,
                         modifier: Boolean,
                         shoot: Int,
                         move: Int,
                         sprint: Int)