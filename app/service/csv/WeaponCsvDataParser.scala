package service.csv

import javax.inject.{Inject, Singleton}
import play.api.Configuration

/**
  * Parses the weapons from the csv and does some validation on it
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class WeaponCsvDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVWeaponDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.FACTIOM_HEADER, CSVHeaders.NAME_HEADER, CSVHeaders.ATTACKS_HEADER)

  override def getFileName(): String = "weapons"

  override def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String],Int)]): Set[CSVWeaponDto] = {
      dataFromCsvFile
        .filter(info => filterRequiredLines(requiredHeaders, info))
        .flatMap(info => {

          val factionName = info._1.get(CSVHeaders.FACTIOM_HEADER)
          val name = info._1.get(CSVHeaders.NAME_HEADER)
          val attacks = readCsvLineToInt(CSVHeaders.ATTACKS_HEADER,info)
          val range = readCsvLineToIntWithDefault(CSVHeaders.RANGE_HEADER, info)
          val armorPiercing = readCsvLineToIntWithDefault(CSVHeaders.ARMOR_PIERCING_HEADER, info)
          val abilities = readCsvLineToSet(CSVHeaders.ABILITIES_HEADER,info)
          val linkedName = info._1.get(CSVHeaders.LINKED_NAME_HEADER)




          if(attacks.isEmpty) {
            None
          } else {
            val weapons = CSVWeaponDto(
              name = name.get,
              factionName = factionName.get,
              range = range,
              attacks = attacks.get,
              armorPiercing = armorPiercing,
              abilities = abilities,
              linkedName = linkedName.getOrElse(name.get)
            )
            Some(weapons)
          }
        })
      .toSet
  }

}


case class CSVWeaponDto(name: String,
                        factionName: String,
                        range: Int,
                        attacks: Int,
                        armorPiercing: Int,
                        abilities: Set[String],
                        linkedName: String)