package service.models

import play.api.Logger
import service.csv.CSVItemDto

import scala.collection.mutable.ListBuffer

/**
  * Handles the items do
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object ItemDao {

  /**
    * All the items
    */
  private val items: ListBuffer[ItemDo] = ListBuffer()

  /**
    * The logger
    */
  private val LOGGER = Logger("ItemsDao")

  /**
    * Adds an item from the csv
    *
    * @param csvItem the csv item to use
    */
  def addItemFromCsv(csvItem: CSVItemDto): Unit = {
    val abilities = AbilitiesDao.findAbilitiesForCsv(csvItem.abilities, (abilityName) => {
      LOGGER.error(s"Cannot find ability: $abilityName for item: ${csvItem.name}")
    })

    val item = ItemDo(name = csvItem.name,
      abilities = abilities,
      defensModifier = csvItem.defenseModifier
    )

    items += item
  }

  /**
    * Gets the items from the csv string
    *
    * @param itemsFromCsv the strings from the csv
    * @param errorLog     the logging callback
    * @return [[Set]] of [[ItemDo]]
    */
  def findItemsForCsv(itemsFromCsv: Set[String], errorLog: (String) => Unit): Set[ItemDo] = {
    itemsFromCsv.flatMap(itemName => {
      if (itemName.isEmpty) {
        None
      } else {
        val itemDo = findItemByName(itemName)
        if (itemDo.isEmpty) {
          errorLog(itemName)
        }
        itemDo
      }
    })
  }

  /**
    * Finds the item by its name
    *
    * @param itemName the name of the item
    * @return the item or none when not found
    */
  def findItemByName(itemName: String): Option[ItemDo] = {
    items
      .find(_.name == itemName)
  }

  /**
    * Deletes all abilities
    */
  def deleteAll(): Unit = {
    items.clear()
  }

}

/**
  * Represents an ability
  *
  * @param name           the name of the ability
  * @param abilities      the abilities the item provides
  * @param defensModifier the defense modifier the item provides
  */
case class ItemDo(name: String,
                  abilities: Set[AbilityWithModifyValueDo],
                  defensModifier: Int)


