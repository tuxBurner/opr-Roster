package service.logic

import java.util.UUID

import akka.Done
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import service.models.{FactionsDao, TroopDao, TroopDo}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


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
    * @param factionName the name of the faction the army belongs to
    * @return [[None]] when something went wrong [[Some]] when everything went alright
    */
  def addNewArmy(factionName: String): Future[Option[ArmyDto]] = {
    // check if the faction exists
    FactionsDao.findFactionByName(factionName)
      .map(factionDo => {
        val uuid = UUID.randomUUID().toString
        val armyDto = ArmyDto(factionName = factionName, uuid = uuid)
        setArmyToCache(armyDto)
          .map(done => Some(armyDto))
      })
      .getOrElse({
        LOGGER.error(s"Cannot add new army the faction: $factionName does not exist")
        Future(None)
      })
  }

  def setArmyToCache(armyDto: ArmyDto): Future[Done] = {
    // TODO: configure duration
    cache.set(armyDto.uuid, armyDto, 15.minutes)
  }


  def getArmy(uuid: String): Future[Option[ArmyDto]] = {
    cache.get(uuid)
  }

  def addTroopToArmy(uuid: String, troopName: String): Future[Option[ArmyDto]] = {
    getArmy(uuid)
      .map(armyOptionFromCache =>
        armyOptionFromCache.map(armyFromCache => {
          // check if we can find the troop for the army faction
          TroopDao.getTroopByNameAndFactionName(troopName = troopName, factionName = armyFromCache.factionName)
            .map(troopDo => {
              val newTroopDto = troopDoToDto(troopDo)
              val updatedArmy = armyFromCache.copy(troops = armyFromCache.troops.appended(newTroopDto))
              Some(updatedArmy)
            })
            .getOrElse({
              LOGGER.error(s"Could not find troop: $troopName for faction: ${armyFromCache.factionName}")
              None
            })
        })
          .getOrElse({
            LOGGER.error(s"Could not add troop: $troopName to army, amry was not found in the cache")
            None
          })
      )
  }

  /**
    * Converts a [[TroopDo]] to a [[TroopDto]]
    *
    * @param troopDo the do to convert
    * @return the converted [[TroopDto]]
    */
  def troopDoToDto(troopDo: TroopDo): TroopDto = {
    TroopDto(uuid = UUID.randomUUID().toString,
      name = troopDo.name,
      size = troopDo.size,
      basicCosts = troopDo.costs,
      basicQuality = troopDo.quality,
      basicDefense = troopDo.defense,
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
                    defense: Int,
                    shoot: Int,
                    fight: Int,
                    move: Int = 3,
                    sprint: Int = 3
                   )