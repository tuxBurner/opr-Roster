import com.google.inject.AbstractModule
import service.DataInitializer
import service.csv.{AbilitiesCsvDataParser, ArmyCSVDataParser, UpgradesCsvDataParser, WeaponCsvDataParser}

/**
  * Module which handles the injection of the self written modules
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ArmyCSVDataParser]).asEagerSingleton()
    bind(classOf[WeaponCsvDataParser]).asEagerSingleton()
    bind(classOf[UpgradesCsvDataParser]).asEagerSingleton()
    bind(classOf[AbilitiesCsvDataParser]).asEagerSingleton()
    bind(classOf[DataInitializer]).asEagerSingleton()
  }

}
