package service.csv

import javax.inject.{Inject, Singleton}
import play.api.Configuration

/**
  * Parses the weapons from the csv and does some validation on it
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class WeaponCsvDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVWeaponDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.FACTIOM_HEADER, CSVHeaders.NAME_HEADER, CSVHeaders.ATTACKS_HEADER)

  override def getFileName(): String = "weapons"

  override def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String], Int)]): Set[CSVWeaponDto] = {
    dataFromCsvFile
      .filter(info => filterRequiredLines(requiredHeaders, info))
      .flatMap(info => {

        val factionName = info._1.get(CSVHeaders.FACTIOM_HEADER)
        val name = info._1.get(CSVHeaders.NAME_HEADER)
        val attacks = readCsvLineToInt(CSVHeaders.ATTACKS_HEADER, info)
        val range = readCsvLineToIntWithDefault(CSVHeaders.RANGE_HEADER, info)
        val armorPiercing = readCsvLineToIntWithDefault(CSVHeaders.ARMOR_PIERCING_HEADER, info)
        val abilities = readCsvLineToList(CSVHeaders.ABILITIES_HEADER, info)
        val linkedName = info._1.get(CSVHeaders.LINKED_NAME_HEADER)


        if (attacks.isEmpty) {
          None
        } else {

          val linkedNameVal = if (linkedName.getOrElse("").isEmpty) name.get else linkedName.get

          val weapons = CSVWeaponDto(
            name = name.get,
            factionName = factionName.get,
            range = range,
            attacks = attacks.get,
            armorPiercing = armorPiercing,
            abilities = abilities,
            linkedName = linkedNameVal
          )
          Some(weapons)
        }
      })
      .toSet
  }

}


/**
  * Represents a weapon from the csv
  *
  * @param name          the name of the weapon
  * @param factionName   the faction the weapon belongs to
  * @param range         the range of the weapon
  * @param attacks       how many attacks does the weapon has
  * @param armorPiercing the armor piercing of the weapon
  * @param abilities     the abilities the weapon brings
  * @param linkedName    when set the weapon is a linked option
  */
case class CSVWeaponDto(name: String,
                        factionName: String,
                        range: Int,
                        attacks: Int,
                        armorPiercing: Int,
                        abilities: List[String],
                        linkedName: String)