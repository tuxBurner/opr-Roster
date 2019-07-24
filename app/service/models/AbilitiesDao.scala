package service.models

import play.api.Logger
import service.csv.CSVAbilityDto

import scala.collection.mutable.ListBuffer

/**
  * Handles the abilities do
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object AbilitiesDao {

  /**
    * All the abilities
    */
  private val abilities: ListBuffer[AbilityDo] = ListBuffer()

  private val LOGGER = Logger("AbilitiesDao")

  /**
    * Tries to find the ability by the given name when it does not exists it will be added
    *
    * @param csvAbilityDto the csv ability to use to add the ability
    * @return the [[AbilityDo]] which was found or newly created
    */
  def findOrAddAbilityFromCsv(csvAbilityDto: CSVAbilityDto): AbilityDo = {
    findAbilityByName(csvAbilityDto.name)
      .getOrElse({
        LOGGER.info(s"Adding new ability: ${csvAbilityDto.name} (${csvAbilityDto.modifier}) shoot: ${csvAbilityDto.shoot} " +
          s"move: ${csvAbilityDto.move} sprint: ${csvAbilityDto.sprint}")
        val abilityDo = AbilityDo(name = csvAbilityDto.name,
          modifier = csvAbilityDto.modifier,
          shoot = csvAbilityDto.shoot,
          move = csvAbilityDto.move,
          sprint = csvAbilityDto.sprint)
        abilities += abilityDo
        abilityDo
      })
  }

  /**
    * Finds the ability by its name
    *
    * @param abilityName the name of the ability
    * @return the ability or none when not found
    */
  def findAbilityByName(abilityName: String): Option[AbilityDo] = {
    abilities
      .find(_.name == abilityName)
  }

  /**
    * Deletes all abilities
    */
  def deleteAll(): Unit = {
    abilities.clear()
  }


  /**
    * This splits an ability with Name(Val)
    *
    * @param abilityString the ability string to split
    * @return (name,modifyValue)
    */
  //TODO: in a helper ?
  def parseAbilityString(abilityString: String): (String, Option[Int]) = {
    val splitAbility = abilityString.split('(')
    val cleanAbilityName = splitAbility(0)
    val abilityModifier = if (splitAbility.length == 2) {
      Some(splitAbility(1).replace(")", "").toInt)
    } else {
      None
    }

    (cleanAbilityName, abilityModifier)
  }

  /**
    * Gets the abilities from the csv string
    *
    * @param abilitiesFromCsv the strings from the csv
    * @param errorLog         the logging callback
    * @return [[List]] of [[AbilityWithModifyValueDo]]
    */
  def findAbilitiesForCsv(abilitiesFromCsv: List[String], errorLog: String => Unit): List[AbilityWithModifyValueDo] = {
    abilitiesFromCsv.flatMap(
      abilityName => {
        if (abilityName.isEmpty) {
          None
        } else {
          val parsedAbilityString = AbilitiesDao.parseAbilityString(abilityName)
          val abilityOption = AbilitiesDao.findAbilityByName(parsedAbilityString._1)
          abilityOption
            .map(ability => Some(AbilityWithModifyValueDo(ability = ability, modifyValue = parsedAbilityString._2)))
            .getOrElse({
              errorLog(parsedAbilityString._1)
              None
            })
        }
      }
    )
  }

}

/**
  * Represents an ability
  *
  * @param name     the name of the ability
  * @param modifier true when the ability has a modifier
  * @param shoot    when not 0 changes the shoot quality of the troop
  * @param move     when not 0 changes the move of the troop
  * @param sprint   when not 0 changes sprint quality of the troop
  */
case class AbilityDo(name: String,
                     modifier: Boolean,
                     shoot: Int,
                     move: Int,
                     sprint: Int)

/**
  * Represents an ability with its modify value
  *
  * @param ability     the ability itself
  * @param modifyValue the value to modify
  */
case class AbilityWithModifyValueDo(ability: AbilityDo,
                                    modifyValue: Option[Int])
