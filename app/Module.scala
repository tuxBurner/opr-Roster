import com.google.inject.AbstractModule
import service.csv.ArmyCSVDataParser

/**
  * Module which handles the injection of the self written modules
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ArmyCSVDataParser]).asEagerSingleton()
  }

}
