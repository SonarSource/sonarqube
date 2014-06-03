package org.sonar.server.log.db;

import org.sonar.core.log.Activity;

/**
 * @since 4.4
 */
public class TestActivity extends Activity {

  public String test;

  public TestActivity(){}

  public TestActivity(String test){
    this.test = test;
  }

  @Override
  public String serialize() {
    return test;
  }

  @Override
  public Activity deSerialize(String data) {
    test = data;
    return this;
  }
}