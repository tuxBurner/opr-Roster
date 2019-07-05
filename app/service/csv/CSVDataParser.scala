package service.csv

import java.io.File

import com.github.tototoshi.csv._
import org.apache.commons.lang3.StringUtils
import play.api.{Configuration, Logger}

import scala.io.Source


/**
  * Base class for parsing the csv information files
  *
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
abstract class CSVDataParser[A](configuration: Configuration) {


  /**
    * Logger to use for the [[CSVDataParser]]
    */
  val LOGGER = Logger(classOf[CSVDataParser[A]])


  /**
    * Parses the data from the csv file to the internal object structure
    *
    * @return [[None]] when an error happened while parsing the data [[Some]] when everything is okay
    */
  def parseData(): Option[A] = {
    val csvData = readCsvFile(getFileName())
    if (csvData.length == 0) {
      LOGGER.error(s"No data found in file: $getFileName()")
      None
    } else {
      Some(readCsvDataInternal(csvData))
    }
  }

  /**
    * Gets the filename of the csv to parse
    *
    * @return the name of the file to open without .csv
    */
  protected def getFileName(): String = ???

  def readCsvDataInternal(dataFromCsvFile: List[(Map[String, String], Int)]): A

  /**
    * Tries to read the given csv file
    *
    * @param fileName the name of the csv file to read
    * @return a List which stores the readed lines of the csv
    */
  private def readCsvFile(fileName: String): List[(Map[String, String], Int)] = {
    val configuredExternalFolder = checkAndGetExternalConfigFolder(configuration)

    val path = s"$fileName.csv"

    implicit object MyFormat extends DefaultCSVFormat {
      override val delimiter = ';'
    }

    val reader = configuredExternalFolder.map(externalConfFolder => {
      val confFile = new File(externalConfFolder, path)
      LOGGER.info(s"Reading configuration from: ${confFile.getAbsolutePath}")
      CSVReader.open(confFile)
    }).getOrElse({
      val currentClassLoader = Thread.currentThread.getContextClassLoader
      val csvIs = currentClassLoader.getResourceAsStream(path)
      CSVReader.open(Source.fromInputStream(csvIs))
    })

    reader
      .allWithHeaders()
      .zipWithIndex

  }

  /**
    * Checks if in the configuration is an external folder specified for the configurations.
    * This feature can be used to create and test the configurations.
    *
    * @return when the file exists the file if not none
    */
  private def checkAndGetExternalConfigFolder(configuration: Configuration): Option[File] = {
    configuration.getOptional[String](f"rosterconfigfolder")
      .map(path => {
        val externalConfFolder = new File(path)
        if (externalConfFolder.exists && externalConfFolder.isDirectory) {
          LOGGER.info(s"Found external config folder: ${externalConfFolder.getAbsolutePath}")
          return Option.apply(externalConfFolder)
        }
        LOGGER.error(s"Could not find configured folder: ${externalConfFolder.getAbsolutePath}")
        return Option.empty
      })
      .getOrElse(Option.empty)
  }

  /**
    * Checks if the required columns exist in the csvInfo and if the value is not blank
    *
    * @param requiredColumns the names of the columns which are required
    * @param csvInfo         the csv info which contains the columns as a map and the current line number
    * @return true when all required columns are set false when not
    */
  protected def filterRequiredLines(requiredColumns: Set[String], csvInfo: (Map[String, String], Int)): Boolean = {
    requiredColumns.forall(column => {
      val colVal = csvInfo._1.get(column)
      if (colVal.isEmpty || StringUtils.isBlank(colVal.get)) {
        LOGGER.error(s"Line ${csvInfo._1.values.mkString} in $getFileName().csv: ${csvInfo._2} has no value at column ${column} set")
        false
      } else {
        true
      }
    })
  }

  /**
    * Reads the value of the column to an [[Int]]
    *
    * @param csvColumn then name of column to parse the value for
    * @param csvInfo   the csv info which contains the columns as a map and the current line number
    * @param logError  when true the error is logged
    * @return [[None]] when the value cannot be converted to an [[Int]] [[Some]] with the int value
    *
    */
  protected def readCsvLineToInt(csvColumn: String, csvInfo: (Map[String, String], Int), logError: Boolean = false): Option[Int] = {
    val stringVal = csvInfo._1.get(csvColumn)
    if (stringVal.isEmpty) {
      return None
    }

    try {
      Some(stringVal.get.toInt)
    } catch {
      case e: Exception => {
        if (logError) {
          LOGGER.error(s"Line in $getFileName().csv: ${csvInfo._2} from colum ${csvColumn} value: ${stringVal.get} is not a number ")
        }
        None
      }
    }
  }

  /**
    * Reads the value of the column to an [[Int]]
    *
    * @param csvColumn  then name of column to parse the value for
    * @param csvInfo    the csv info which contains the columns as a map and the current line number
    * @param defaultVal the default val to set when nothing was inm the column
    * @return the default val when the value cannot be converted to an [[Int]]
    */
  protected def readCsvLineToIntWithDefault(csvColumn: String, csvInfo: (Map[String, String], Int), defaultVal: Int = 0): Int = {
    readCsvLineToInt(csvColumn, csvInfo, logError = false).getOrElse(defaultVal)
  }


  /**
    * Gets the values of the csv column as a [[Set]] the value is splitted by the character
    *
    * @param csvColumn the name of the csv column to get the data for
    * @param csvInfo   the csv info which contains the columns as a map and the current line number
    * @return a [[Set]] with the values
    */
  protected def readCsvLineToSet(csvColumn: String, csvInfo: (Map[String, String], Int), splitChar: String = ","): Set[String] = {
    csvInfo._1.get(csvColumn)
      .map(_.trim.split(splitChar).map(_.trim).toSet)
      .getOrElse({
        Set.empty
      })
  }

  /**
    * Gets the values of the csv column as a [[List]] the value is splitted by the character
    *
    * @param csvColumn the name of the csv column to get the data for
    * @param csvInfo   the csv info which contains the columns as a map and the current line number
    * @return a [[List]] with the values
    */
  protected def readCsvLineToList(csvColumn: String, csvInfo: (Map[String, String], Int), splitChar: String = ","): List[String] = {
    csvInfo._1.get(csvColumn)
      .map(_.trim.split(splitChar).map(_.trim).toList)
      .getOrElse({
        List.empty
      })
  }

}

