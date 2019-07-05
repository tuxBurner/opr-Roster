package service.csv

import javax.inject.{Inject, Singleton}
import org.apache.commons.lang3.StringUtils
import play.api.Configuration

/**
  * Parses the upgrades from the csv and does some validation on it
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
@Singleton
class UpgradesCsvDataParser @Inject()(configuration: Configuration) extends CSVDataParser[Set[CSVUpgradeDto]](configuration) {

  val requiredHeaders = Set(CSVHeaders.FACTIOM_HEADER, CSVHeaders.UPGRADES_HEADER, CSVHeaders.COST_HEADER, CSVHeaders.WITH_HEADER, CSVHeaders.WITH_TYPE_HEADER)


  override def getFileName(): String = "upgrades"

  override def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String], Int)]): Set[CSVUpgradeDto] = {
    val filteredUpdates = dataFromCsvFile
      .filter(info => filterRequiredLines(requiredHeaders, info))


    // split the upgrades by
    val upgrades = split(filteredUpdates, { x: (Map[String, String], Int) => {
      val rule = x._1.get(CSVHeaders.RULE_HEADER)
      rule.isDefined && StringUtils.isNotBlank(rule.get)
    }
    })

    val factions = upgrades.map(_ (0)._1(CSVHeaders.FACTIOM_HEADER)).toSet


    val updatesDto = factions.flatMap(faction => {
      val upgradesForFaction = upgrades.filter(_ (0)._1.get(CSVHeaders.FACTIOM_HEADER).get == faction)
      val groupedByUpgrades = upgradesForFaction.groupBy(_ (0)._1.get(CSVHeaders.UPGRADES_HEADER).get)

      groupedByUpgrades.keys.map(updateName => {
        val rules = groupedByUpgrades.get(updateName).get

        val rulesDto = rules.map(ruleLines => {
          val firstLine = ruleLines(0)
          val ruleName = firstLine._1.get(CSVHeaders.RULE_HEADER).get
          val subjects = readCsvLineToSet(CSVHeaders.SUBJECTS_HEADER, firstLine)
          val amount = readCsvLineToIntWithDefault(CSVHeaders.AMOUNT_HEADER, firstLine)

          val upgradeWiths = ruleLines.map(ruleLine => {
            val costs = readCsvLineToInt(CSVHeaders.COST_HEADER, ruleLine)

            val upgradeWith = readCsvLineToList(CSVHeaders.WITH_HEADER, ruleLine)
            val upgradeWithType = readCsvLineToList(CSVHeaders.WITH_TYPE_HEADER, ruleLine)

            val upgradeWithSet: Set[CSVUpgradeWithDTO] = if (upgradeWith.size != upgradeWithType.size) {
              LOGGER.error(s"The ${CSVHeaders.WITH_HEADER} and ${CSVHeaders.WITH_TYPE_HEADER} at line: ${ruleLine._2} in the $getFileName().csv don't have the same amount of options.")
              Set.empty
            } else {
              upgradeWith.zipWithIndex.map((upgradeWithBlock) => CSVUpgradeWithDTO(withName = upgradeWithBlock._1, withType = upgradeWithType(upgradeWithBlock._2))).toSet
            }

            CSVUpgradeOptionsDto(costs = costs.get,
              upgradeWith = upgradeWithSet)
          }).toSet

          CSVUpgradeRuleDto(rule = ruleName,
            subjects = subjects,
            amount = amount,
            options = upgradeWiths)
        }).toSet

        CSVUpgradeDto(name = updateName,
          factionName = faction,
          rules = rulesDto)

      })
    })


    updatesDto
  }


  def split[A](list_in: List[A], search: A => Boolean): List[List[A]] = {

    def split_helper(accum: List[List[A]], list_in2: List[A], search: A => Boolean): List[List[A]] = {


      val startLines = list_in2.head
      val withoutStart = list_in2.drop(1)

      val blockLines = withoutStart.takeWhile(search(_) == false)

      val finalList = withoutStart.drop(blockLines.length)

      val fullBlockLines = startLines :: blockLines

      val new_accum = accum :+ fullBlockLines
      if (finalList.length > 0) {
        split_helper(new_accum, finalList, search)
      } else {
        new_accum
      }

    }

    split_helper(List(), list_in, search)
  }

}


case class CSVUpgradeDto(name: String,
                         factionName: String,
                         rules: Set[CSVUpgradeRuleDto])

case class CSVUpgradeRuleDto(rule: String,
                             subjects: Set[String],
                             amount: Int,
                             options: Set[CSVUpgradeOptionsDto])

case class CSVUpgradeOptionsDto(costs: Int,
                                upgradeWith: Set[CSVUpgradeWithDTO])

case class CSVUpgradeWithDTO(withName: String,
                             withType: String)