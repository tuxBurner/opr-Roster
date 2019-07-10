package service.csv

import javax.inject.{Inject, Singleton}
import play.api.Configuration

/**
  * Parses the items from the csv and does some validation on it
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class ItemsCsvDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVItemDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.NAME_HEADER)

  override def getFileName(): String = "items"

  override def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String], Int)]): Set[CSVItemDto] = {
    dataFromCsvFile
      .filter(info => filterRequiredLines(requiredHeaders, info))
      .map(info => {
        val name = info._1.get(CSVHeaders.NAME_HEADER).get
        val defense = readCsvLineToIntWithDefault(CSVHeaders.DEFENSE_HEADER, info, 0)
        val abilities = readCsvLineToSet(CSVHeaders.ABILITIES_HEADER, info)
        CSVItemDto(name = name,
          abilities = abilities,
          defenseModifier = defense)
      }).toSet
  }
}

/**
  * Represents an item from the csv
  *
  * @param name            the name of the ability
  * @param abilities       the abilities this item brings
  * @param defenseModifier the defense modifier
  */
case class CSVItemDto(name: String,
                      abilities: Set[String],
                      defenseModifier: Int)
