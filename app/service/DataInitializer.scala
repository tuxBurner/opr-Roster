package service

import javax.inject.{Inject, Singleton}
import service.csv._
import service.models.{AbilitiesDao, FactionDo, FactionsDao, TroopDao, WeaponDao}

/**
  * This calls the csv parsers does some checking on it and inserts the data into the daos
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class DataInitializer @Inject()(armiesCsvParser: ArmyCSVDataParser,
                                weaponsCsvParser: WeaponCsvDataParser,
                                upgradesCsvParser: UpgradesCsvDataParser,
                                abilitiesCsvParser: AbilitiesCsvDataParser) {

  /**
    * Inits the data for the roster from the csvs
    */
  def initData(): Unit = {
    val csvArmyInfos = armiesCsvParser.parseData()
    val csvWeaponInfos = weaponsCsvParser.parseData()
    val csvUpgradeInfos = upgradesCsvParser.parseData()
    val csvAbilitiesInfos = abilitiesCsvParser.parseData()


    addFactions(csvArmyInfos.get)
    addAbilities(csvAbilitiesInfos.get)
    addWeapons(csvWeaponInfos.get)
    addTroops(csvArmyInfos.get)


  }

  /**
    * Adds factions to the database
    *
    * @param csvFactions the csv factions to add
    */
  def addFactions(csvFactions: Set[CSVFactionDto]): Unit = {
    FactionsDao.deleteAll()
    csvFactions.foreach(csvFaction => {
      FactionsDao.findOrAddFaction(csvFaction.name)
    })
  }

  /**
    * Adds all troops
    * @param csvFactions the csv factions containing there troops
    */
  def addTroops(csvFactions: Set[CSVFactionDto]) : Unit = {
    TroopDao.deleteAll()
    csvFactions.foreach(csvFaction => {
      val factionDo = FactionsDao.findFactionByName(csvFaction.name).get
      addTroopsToFaction(csvFaction.troops,factionDo)
    })
  }

  /**
    * Adds all the abilities from the csv
    *
    * @param csvAbilities the abilities from the csv to add
    */
  def addAbilities(csvAbilities: Set[CSVAbilityDto]): Unit = {

    AbilitiesDao.deleteAll()
    csvAbilities.foreach(csvAbility => AbilitiesDao.findOrAddAbility(csvAbility.name, csvAbility.modifier))
  }

  /**
    * Adds all the weapons from the csv
    *
    * @param csvWeapons the weapons from the csv
    */
  def addWeapons(csvWeapons: Set[CSVWeaponDto]): Unit = {
    WeaponDao.deletAll()
    csvWeapons.foreach(WeaponDao.addWeaponFromCSV)
  }

  /**
    * Adds the troops to the csv faction
    *
    * @param csvFaction
    */
  def addTroopsToFaction(csvTroops: Set[CSVTroopDto], faction: FactionDo): Unit = {
    csvTroops.foreach(TroopDao.addTroopFromCsvDto(_,faction))
  }

}
