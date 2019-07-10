package service.models;

/**
 * The type of a with
 */
enum EUpgradeWithType {


  /**
   * Ability upgrade with type
   */
  ABILITY("A"),

  /**
   * ITEM upgrade with type
   */
  ITEM("I"),

  /**
   * WEAPON upgrade with type
   */
  WEAPON("W");


  /**
   * The csv key
   */
  public final String csvKey;

  /**
   * Constructor for the {@link EUpgradeWithType}
   *
   * @param csvKey the key in the csv
   */
  EUpgradeWithType(String csvKey) {
    this.csvKey = csvKey;
  }

}
