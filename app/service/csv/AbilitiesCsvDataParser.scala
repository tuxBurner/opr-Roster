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
        val name = info._1.get(CSVHeaders.NAME_HEADER).get
        val modifier = info._1.get(CSVHeaders.MODIFIER_HEADER).get.toUpperCase == "X"
        Some(CSVAbilityDto(name = name, modifier = modifier))
      }).toSet
  }
}

/**
  * Represents an ability from the csv
  * @param name the name of the ability
  * @param modifier if the ability has a modifier
  */
case class CSVAbilityDto(name: String,
                         modifier: Boolean)