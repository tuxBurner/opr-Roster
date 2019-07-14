import com.google.inject.AbstractModule
import service.DataInitializer
import service.csv.{AbilitiesCsvDataParser, ArmyCSVDataParser, UpgradesCsvDataParser, WeaponCsvDataParser}
import service.logic.{ArmyLogic, UpgradeLogic}

/**
  * Module which handles the injection of the self written modules
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class Module extends AbstractModule {

  override def configure(): Unit = {
    // bind the csv parsers
    bind(classOf[ArmyCSVDataParser]).asEagerSingleton()
    bind(classOf[WeaponCsvDataParser]).asEagerSingleton()
    bind(classOf[UpgradesCsvDataParser]).asEagerSingleton()
    bind(classOf[AbilitiesCsvDataParser]).asEagerSingleton()

    // bind the data initializer
    bind(classOf[DataInitializer]).asEagerSingleton()

    // bind the logics
    bind(classOf[ArmyLogic]).asEagerSingleton()
    bind(classOf[UpgradeLogic]).asEagerSingleton()
  }

}
