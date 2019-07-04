package service.csv

import java.io.File

import play.api.{Configuration, Logger}
import com.github.tototoshi.csv._

import scala.io.Source


/**
  * Base class for parsing the csv information files
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
abstract class CSVDataParser[A](configuration: Configuration) {

  //self:A =>

  /**
    * Logger to use for the [[CSVDataParser]]
    */
  val LOGGER = Logger(classOf[CSVDataParser[A]])


  def parseData() : Option[A] = {
    val csvData = readCsvFile(getFileName())
    if(csvData.length == 0) {
      LOGGER.error(s"No data found in file: $getFileName()")
      None
    } else {
      Some(readCsvDataInternal(csvData))
    }
  }

  def getFileName(): String

  def readCsvDataInternal(dataFromCsvFile : List[Map[String, String]] ) : A

  /**
    * Tries to read the given csv file
    * @param fileName the name of the csv file to read
    * @return a List which stores the readed lines of the csv
    */
  private def readCsvFile(fileName: String): List[Map[String, String]] = {
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

    reader.allWithHeaders()

  }

  /**
    * Checks if in the configuration is an external folder specified for the configurations.
    * This feature can be used to create and test the configurations.
    *
    * @return
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

}

