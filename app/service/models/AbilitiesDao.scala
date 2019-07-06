package service.models

import play.api.Logger

import scala.collection.mutable.ListBuffer

/**
  * Handles the abilities do
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object AbilitiesDao {

  val abilities: ListBuffer[AbilityDo] = ListBuffer()

  private val LOGGER = Logger("AbilitiesDao")

  /**
    * Tries to find the ability by the given name when it does not exists it will be added
    *
    * @param name     the name of the ability
    * @param modifier true when the ability has a modifier
    * @return the [[AbilityDo]] which was found or newly created
    */
  def findOrAddAbility(name: String, modifier: Boolean): AbilityDo = {
    findAbilityByName(name)
      .getOrElse({
        LOGGER.info(s"Adding new ability: $name ($modifier)")
        val abilityDo = AbilityDo(name = name, modifier = modifier)
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

}

/**
  * Represents an ability
  *
  * @param name     the name of the ability
  * @param modifier true when the ability has a modifier
  */
case class AbilityDo(name: String,
                     modifier: Boolean)
