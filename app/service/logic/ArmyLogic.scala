package service.logic

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import service.models._

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


  /**
    * Sets the amount of the troop in the army
    *
    * @param armyUuid  the uuid of the army
    * @param troopUuid the uuid of the troop to set the amount for
    * @param amount    the new amount to set
    * @return the updated army
    */
  def setTroopAmount(armyUuid: String, troopUuid: String, amount: Int): Future[Option[ArmyDto]] = {
    getArmyAndTroopForUpdate(armyUuid, troopUuid, (army, troop) => {
      if (amount <= 0) {
        LOGGER.error(s"Cannot set amount: $amount <= 0 on troop: $troopUuid in army: $armyUuid")
        Some(army)
      } else {
        val newTroops = army.troops.map(troopInArmy => {
          if (troopInArmy.uuid != troop.uuid) {
            troopInArmy
          } else {
            troopInArmy.copy(amount = amount)
          }
        })

        Some(army.copy(troops = newTroops, totalCosts = calcTotalArmyCosts(newTroops)))
      }
    })
  }

  /**
    * Sets the nam on the army
    *
    * @param armyUuid the uuid of the army
    * @param armyName the name of the army to set
    * @return the army with the new name
    */
  def setArmyName(armyUuid: String, armyName: String): Future[Option[ArmyDto]] = {
    getArmy(armyUuid)
      .map(armyOption => {
        armyOption.map(armyFromCache => armyFromCache.copy(name = armyName))
      })
  }


  /**
    * Removes the given troop from the given army
    *
    * @param armyUuid  the uuid of the army where to remove the troop from
    * @param troopUuid the uuid of the troop to remove from the army
    * @return the army without the troop
    */
  def removeTroopFromArmy(armyUuid: String, troopUuid: String): Future[Option[ArmyDto]] = {
    getArmyAndTroopForUpdate(armyUuid, troopUuid, (army, troop) => {
      val newTroops = army.troops.filterNot(_.uuid == troop.uuid)
      Some(army.copy(troops = newTroops, totalCosts = calcTotalArmyCosts(newTroops)))
    })
  }

  /**
    * Gets the army by the uuid and the troop in the army by the uuid and when found both of them applies them on the changeArmyFun
    *
    * @param armyUuid      the uuid of the army
    * @param troopUuid     the troop uuid
    * @param changeArmyFun function to apply on the army and troop when both where found
    * @return [[None]] when the army was not found else [[ArmyDto]] which was changed or not
    */
  def getArmyAndTroopForUpdate(armyUuid: String, troopUuid: String, changeArmyFun: (ArmyDto, TroopDto) => Option[ArmyDto]): Future[Option[ArmyDto]] = getArmy(armyUuid)
    .map(armyOption => {
      armyOption.map(army => {
        // check if the troop is in the army an when so apply the changeArmyFun
        army
          .troops
          .find(_.uuid == troopUuid)
          .map(troop => {
            changeArmyFun(army, troop)
          })
          .getOrElse({
            LOGGER.error(s"Cannot perform change on troop: $troopUuid was not found in army: $armyUuid")
            Some(army)
          })
      })
        .getOrElse({
          LOGGER.error(s"Could not perform change on army: $armyUuid was not found in cache")
          None
        })
    })


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
    applyTroopChangeValues(TroopDto(uuid = UUID.randomUUID().toString,
      name = troopDo.name,
      size = troopDo.size,
      basicCosts = troopDo.costs,
      basicQuality = troopDo.quality,
      basicDefense = troopDo.defense,
      costs = troopDo.costs,
      currentAbilities = troopDo.defaultAbilities.map(abilityDoToDto),
      currentItems = Set.empty,
      currentWeapons = troopDo.defaultWeapons.map(weaponDoToDto),
      possibleUpgrades = troopDo.upgrades
    ))
  }

  /**
    * Goes through the items, abilities etc and checks if the values of the troop are changing
    *
    * @param troopDto the troop to check on
    * @return the troop with the changed values
    */
  private def applyTroopChangeValues(troopDto: TroopDto): TroopDto = {
    // check if we have an ability which modifies the shoot quality
    val shoot = troopDto.currentAbilities
      .find(_.shoot > 0)
      .map(ability => {
        LOGGER.info(s"Changed troop: ${troopDto.name} shoot from: ${troopDto.basicQuality} to: ${ability.shoot} cause of ability: ${ability.name}")
        ability.shoot
      })
      .getOrElse(troopDto.basicQuality)

    // check if we have an ability which modifies the move
    val move = troopDto.currentAbilities
      .find(_.move > 0)
      .map(ability => {
        LOGGER.info(s"Changed troop: ${troopDto.name} move from: ${troopDto.move} to: ${ability.move} cause of ability: ${ability.name}")
        ability.move
      })
      .getOrElse(6)

    // check if we have an ability which modifies the move
    val sprint = troopDto.currentAbilities
      .find(_.sprint > 0)
      .map(ability => {
        LOGGER.info(s"Changed troop: ${troopDto.name} sprint from: ${troopDto.sprint} to: ${ability.sprint} cause of ability: ${ability.name}")
        ability.sprint
      })
      .getOrElse(12)

    // check if the troop has an item which modifies the defense
    val defense = troopDto.currentItems
      .find(_.defenseModifier > 0)
      .map(item => {
        LOGGER.info(s"Changed troop: ${troopDto.name} defense from: ${troopDto.basicDefense} to: ${item.defenseModifier} cause of item: ${item.name}")
        item.defenseModifier
      })
      .getOrElse(troopDto.basicDefense)

    troopDto.copy(shoot = shoot,
      defense = defense,
      move = move,
      sprint = sprint)
  }


  /**
    * Converts the given [[WeaponDo]] to a [[WeaponDto]]
    *
    * @param weaponDo the weapon do to convert
    * @return the converted [[WeaponDto]]
    */
  private def weaponDoToDto(weaponDo: WeaponDo): WeaponDto = {
    WeaponDto(name = weaponDo.name,
      linkedName = weaponDo.linkedName,
      range = weaponDo.range,
      attacks = weaponDo.attacks,
      armorPiercing = weaponDo.armorPiercing,
      abilities = weaponDo.abilities.map(abilityDoToDto))
  }

  /**
    * Converts the given [[AbilityWithModifyValueDo]] to a [[AbilityDto]]
    *
    * @param abilityDo the ability do to convert
    * @return the converted [[AbilityDto]]
    */
  private def abilityDoToDto(abilityDo: AbilityWithModifyValueDo): AbilityDto = {
    AbilityDto(name = abilityDo.ability.name,
      modifier = abilityDo.modifyValue,
      shoot = abilityDo.ability.shoot,
      move = abilityDo.ability.move,
      sprint = abilityDo.ability.sprint)
  }

  /**
    * Converts the given [[ItemDo]] to a [[ItemDto]]
    *
    * @param itemDo the item do to convert
    * @return the converted [[ItemDto]]
    */
  private def itemDoToDto(itemDo: ItemDo): ItemDto = {
    ItemDto(name = itemDo.name,
      abilities = itemDo.abilities.map(abilityDoToDto),
      defenseModifier = itemDo.defensModifier)
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
  * @param uuid             the uuid of the troop
  * @param name             the name of the troop
  * @param amount           how many are in the army
  * @param size             the size of the troop
  * @param basicCosts       the initial costs of the troop
  * @param basicQuality     the basic quality stat of the troop
  * @param basicDefense     the basic defense stat of the troop
  * @param costs            the current costs value which the troop has after applying all updates to it
  * @param defense          the current defense value which the troop has after applying all updates to it
  * @param shoot            the current shoot value which the troop has after applying all updates to it
  * @param fight            the current fight value which the troop has after applying all updates to it
  * @param move             the current move value which the troop has after applying all updates to it
  * @param sprint           the current sprint value which the troop has after applying all updates to it
  * @param currentAbilities the current abilities the troop has
  * @param currentItems     the current items the troop has
  * @param currentWeapons   the current weapons the troop has
  * @param possibleUpgrades the names of the upgrades the troop can use
  */
case class TroopDto(uuid: String,
                    name: String,
                    amount: Int = 1,
                    size: Int,
                    basicCosts: Int,
                    basicQuality: Int,
                    basicDefense: Int,
                    costs: Int,
                    defense: Int = 0,
                    shoot: Int = 0,
                    fight: Int = 0,
                    move: Int = 6,
                    sprint: Int = 12,
                    currentWeapons: Set[WeaponDto],
                    currentAbilities: Set[AbilityDto],
                    currentItems: Set[ItemDto],
                    possibleUpgrades: Set[String]
                   )

/**
  * Represents a weapon
  *
  * @param name          the name of the weapon
  * @param linkedName    the linked name of the weapon
  * @param range         the range of the weapon
  * @param attacks       the attacks the weapon has
  * @param armorPiercing the armor piercing of the weapon
  * @param abilities     the abilities of the weapon
  */
case class WeaponDto(name: String,
                     linkedName: String,
                     range: Int,
                     attacks: Int,
                     armorPiercing: Int,
                     abilities: Set[AbilityDto])

/**
  * Represtents an item
  *
  * @param name            the name of the item
  * @param abilities       the abilities the item provides
  * @param defenseModifier when [[Some]] how mich does the item modify the defense of the troop
  */
case class ItemDto(name: String,
                   abilities: Set[AbilityDto],
                   defenseModifier: Int)

/**
  * Represents an ability
  *
  * @param name     the name of the ability
  * @param modifier the modify value of the ability
  * @param shoot    when not 0 the shoot quality of the troop is changed
  * @param move     when not 0 the move of the troop is changed
  * @param sprint   when not 0 the sprint of the troop is changed
  */
case class AbilityDto(name: String,
                      modifier: Option[Int],
                      shoot: Int,
                      move: Int,
                      sprint: Int)
