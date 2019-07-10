package service

import javax.inject.{Inject, Singleton}
import service.csv._
import service.models._

/**
  * This calls the csv parsers does some checking on it and inserts the data into the daos
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class DataInitializer @Inject()(armiesCsvParser: ArmyCSVDataParser,
                                weaponsCsvParser: WeaponCsvDataParser,
                                upgradesCsvParser: UpgradesCsvDataParser,
                                abilitiesCsvParser: AbilitiesCsvDataParser,
                                itemsCsvParser: ItemsCsvDataParser) {

  /**
    * Inits the data for the roster from the csvs
    */
  def initData(): Unit = {
    val csvArmyInfos = armiesCsvParser.parseData()
    val csvWeaponInfos = weaponsCsvParser.parseData()
    val csvUpgradeInfos = upgradesCsvParser.parseData()
    val csvAbilitiesInfos = abilitiesCsvParser.parseData()
    val csvItems = itemsCsvParser.parseData()

    addFactions(csvArmyInfos.get)
    addAbilities(csvAbilitiesInfos.get)
    addWeapons(csvWeaponInfos.get)
    addItems(csvItems.get)
    addTroops(csvArmyInfos.get)
    addUpgrades(csvUpgradeInfos.get)
  }

  /**
    * Adds factions to the database
    *
    * @param csvFactions the csv factions to add
    */
  private def addFactions(csvFactions: Set[CSVFactionDto]): Unit = {
    FactionsDao.deleteAll()
    csvFactions.foreach(csvFaction => {
      FactionsDao.findOrAddFaction(csvFaction.name)
    })
  }

  /**
    * Adds all troops
    *
    * @param csvFactions the csv factions containing there troops
    */
  private def addTroops(csvFactions: Set[CSVFactionDto]): Unit = {
    TroopDao.deleteAll()
    csvFactions.foreach(csvFaction => {
      val factionDo = FactionsDao.findFactionByName(csvFaction.name).get
      addTroopsToFaction(csvFaction.troops, factionDo)
    })
  }

  /**
    * Adds all the abilities from the csv
    *
    * @param csvAbilities the abilities from the csv to add
    */
  private def addAbilities(csvAbilities: Set[CSVAbilityDto]): Unit = {

    AbilitiesDao.deleteAll()
    csvAbilities.foreach(csvAbility => AbilitiesDao.findOrAddAbility(csvAbility.name, csvAbility.modifier))
  }

  /**
    * Adds all the weapons from the csv
    *
    * @param csvWeapons the weapons from the csv
    */
  private def addWeapons(csvWeapons: Set[CSVWeaponDto]): Unit = {
    WeaponDao.deletAll()
    csvWeapons.foreach(WeaponDao.addWeaponFromCSV)
  }

  /**
    * Adds the troops to the csv faction
    *
    * @param csvTroops the troops from the csv
    * @param faction   the faction the troops belong to
    */
  private def addTroopsToFaction(csvTroops: Set[CSVTroopDto], faction: FactionDo): Unit = {
    csvTroops.foreach(TroopDao.addTroopFromCsvDto(_, faction))
  }

  /**
    * Adds all the csv upgrades
    *
    * @param csvUpgrades the csv upgrades
    */
  private def addUpgrades(csvUpgrades: Set[CSVUpgradeDto]): Unit = {
    UpgradesDao.deletAll()
    csvUpgrades.foreach(UpgradesDao.addUpgradeFromCsv)
  }

  /**
    * Adds all items from the csv items
    *
    * @param csvItems the items from the csv
    */
  private def addItems(csvItems: Set[CSVItemDto]): Unit = {
    ItemDao.deleteAll()
    csvItems.foreach(ItemDao.addItemFromCsv)
  }

}
