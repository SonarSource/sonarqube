package org.sonar.core.log;

/**
 * @since 4.4
 */

public abstract class Activity {

  public Activity(){}

  public String getName(){
    return this.getClass().getSimpleName();
  }

  public abstract String serialize();

  public abstract Activity deSerialize(String data);
}
