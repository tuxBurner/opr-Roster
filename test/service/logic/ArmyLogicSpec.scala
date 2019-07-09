package service.logic

import org.scalatest.AsyncFlatSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder


/**
  * Tests for the army logic
  */
class ArmyLogicSpec extends AsyncFlatSpec {


  val fakeApplication: Application = new GuiceApplicationBuilder().build()

  val armyLogic: ArmyLogic = fakeApplication.injector.instanceOf(classOf[ArmyLogic])

  behavior of "ArmyLogic"

  it should "return empty when army is not present" in {
    val armyNotKnown = armyLogic.getArmy("asdasasd")
    armyNotKnown.map(muuh => {
      assert(muuh.isEmpty)
    })
  }


}
