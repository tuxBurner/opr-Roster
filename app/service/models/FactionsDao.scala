package service.models

import play.api.Logger

import scala.collection.mutable.ListBuffer

/**
  * Handles the factions do
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object FactionsDao {

  /**
    * All the factions
    */
  private val LOGGER = Logger("FactionDao")

  private val factions: ListBuffer[FactionDo] = ListBuffer()

  /**
    * Tries to find the faction when it does not exists it adds it to the list
    * @param factionName the name of the faction
    * @return the faction which was found or was added
    */
  def findOrAddFaction(factionName: String): FactionDo = {
    findFactionByName(factionName)
      .getOrElse({
        LOGGER.info(s"Adding faction: $factionName")
        val factionDo = FactionDo(name = factionName)
        factions += factionDo
        factionDo
      })
  }

  /**
    * Finds the faction by its name
    * @param factionName the name of the faction to look for
    * @return the faction or none when not found
    */
  def findFactionByName(factionName: String) : Option[FactionDo] = {
    factions
      .find(_.name.equals(factionName))
  }

  /**
    * Returns all factions ordered by there name
    * @return
    */
  def getAllFactionNamesOrderd(): List[String] = {
    factions
      .map(_.name)
      .sorted
      .toList
  }

  /**
    * Deletes all factions
    */
  def deleteAll() : Unit = {
    factions.clear()
  }


}

/**
  * Faction do
  * @param name the name of the faction
  * @param troops the troops the faction has
  */
case class FactionDo(name: String,
                     troops: Set[TroopDo] = Set.empty)
