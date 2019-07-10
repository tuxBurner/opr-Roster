package service.logic

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import service.models.{FactionsDao, TroopDao, TroopDo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Handles all the logic which is performed on the army
  */
@Singleton
class ArmyLogic @Inject()(cache: AsyncCacheApi) {

  /**
    * Logger for the army logic
    */
  val LOGGER = Logger(classOf[ArmyLogic])

  /**
    * Adds a new army
    *
    * @param uuid        the uuid of the army
    * @param factionName the name of the faction the army belongs to
    * @return [[None]] when something went wrong [[Some]] when everything went alright
    */
  def addNewArmy(uuid: String, factionName: String): Future[Option[ArmyDto]] = {
    // check if the faction exists
    Future(FactionsDao.findFactionByName(factionName)
      .map(_ => {
        val armyDto = ArmyDto(factionName = factionName, uuid = uuid)
        Some(setArmyToCache(armyDto))
      })
      .getOrElse({
        LOGGER.error(s"Cannot add new army the faction: $factionName does not exist")
        None
      }))
  }

  /**
    * Sets the army to the cache
    *
    * @param armyDto the army to set to the cache
    * @return the army which was set to the cache
    *         TODO: Make it async
    */
  def setArmyToCache(armyDto: ArmyDto): ArmyDto = {
    // TODO: configure duration
    cache.sync.set(armyDto.uuid, armyDto, 15.minutes)
    armyDto
  }


  /**
    * Gets the army from the cache
    *
    * @param uuid the uuid of the army
    * @return the army from the cache or [[None]] when not found
    */
  def getArmy(uuid: String): Future[Option[ArmyDto]] = {
    cache.get(uuid)
  }

  /**
    * Adds a troop to the army
    *
    * @param uuid      the uuid of the army
    * @param troopName the nam of the troop to add
    * @return the updated or not army
    */
  def addTroopToArmy(uuid: String, troopName: String): Future[Option[ArmyDto]] = {
    getArmy(uuid)
      .map(armyOptionFromCache =>
        armyOptionFromCache.map(armyFromCache => {
          // check if we can find the troop for the army faction
          TroopDao.getTroopByNameAndFactionName(troopName = troopName, factionName = armyFromCache.factionName)
            .map(troopDo => {
              val newTroopDto = troopDoToDto(troopDo)

              val newTroops = armyFromCache.troops :+ newTroopDto
              val armyCosts = calcTotalArmyCosts(newTroops)

              val updatedArmyWithTroops = armyFromCache.copy(troops = newTroops, totalCosts = armyCosts)


              Some(setArmyToCache(updatedArmyWithTroops))
            })
            .getOrElse({
              LOGGER.error(s"Could not find troop: $troopName for faction: ${armyFromCache.factionName}")
              Some(armyFromCache)
            })
        })
          .getOrElse({
            LOGGER.error(s"Could not add troop: $troopName to army, army was not found in the cache")
            None
          })
      )
  }

  /*def setTroopAmount(armyUuid: String, troopUuid: String, amount: Int) : Future[Option[ArmyDto]] = {

   Future(None)
  } */


  /*if(amount <= 0) {
    LOGGER.error(s"Cannot set amount: $amount <= 0 on troop: $troopUuid in army: $armyUuid")
  } *(
}

def removeTroopFromArmy(armyUuid: String, troopUuid: String) : Future[Option[ArmyDto]] = {

}*/


  /**
    * Caclulats the total cost of the troops
    *
    * @param troops the troops to calculate the total cost on
    * @return the total cost of the troops
    */
  private def calcTotalArmyCosts(troops: List[TroopDto]): Int = {
    troops.map(troopDto => troopDto.costs * troopDto.amount).sum
  }

  /**
    * Converts a [[TroopDo]] to a [[TroopDto]]
    *
    * @param troopDo the do to convert
    * @return the converted [[TroopDto]]
    */
  private def troopDoToDto(troopDo: TroopDo): TroopDto = {
    TroopDto(uuid = UUID.randomUUID().toString,
      name = troopDo.name,
      size = troopDo.size,
      basicCosts = troopDo.costs,
      basicQuality = troopDo.quality,
      basicDefense = troopDo.defense,
      costs = troopDo.costs,
      defense = troopDo.defense,
      shoot = troopDo.quality,
      fight = troopDo.quality
    )
  }


}


/**
  * Represents an army a user has created
  *
  * @param factionName the name of the faction the army belongs to
  * @param name        the name of the army can be empty
  * @param uuid        the uuid of the army which is used for the cache
  * @param totalCosts  the total cost of the army
  * @param troops      the troops which belong to the army
  */
case class ArmyDto(factionName: String,
                   name: String = "",
                   uuid: String,
                   totalCosts: Int = 0,
                   troops: List[TroopDto] = List())

/**
  * Represents a troop which belong to an army
  *
  * @param uuid         the uuid of the troop
  * @param name         the name of the troop
  * @param amount       how many are in the army
  * @param size         the size of the troop
  * @param basicCosts   the initial costs of the troop
  * @param basicQuality the basic quality stat of the troop
  * @param basicDefense the basic defense stat of the troop
  * @param costs        the current costs value which the troop has after applying all updates to it
  * @param defense      the current defense value which the troop has after applying all updates to it
  * @param shoot        the current shoot value which the troop has after applying all updates to it
  * @param fight        the current fight value which the troop has after applying all updates to it
  * @param move         the current move value which the troop has after applying all updates to it
  * @param sprint       the current sprint value which the troop has after applying all updates to it
  */
case class TroopDto(uuid: String,
                    name: String,
                    amount: Int = 1,
                    size: Int,
                    basicCosts: Int,
                    basicQuality: Int,
                    basicDefense: Int,
                    costs: Int,
                    defense: Int,
                    shoot: Int,
                    fight: Int,
                    move: Int = 3,
                    sprint: Int = 3
                   )
