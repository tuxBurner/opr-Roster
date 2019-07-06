package service.models

/**
  * Handles the troop dos
  * @author Sebastian Hardt (s.hardt@micromata.de)
  */
object TroopDao {

}




case class TroopDo(name: String,
                   faction: FactionDo,
                   size: Int,
                   quality: Int,
                   costs: Int,
                   defaultWeapons: Set[WeaponDo],
                   defaultAbilities: Set[AbilityDo])
