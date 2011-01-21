package org.sonar.api.resources;

/**
 * @since 2.6
 */
public interface Natures {

  /**
   * Everything which relate to source code (for example "src/main/java" and "src/main/resources").
   */
  String MAIN = "MAIN";

  /**
   * Everything which relate to unit tests (for example "src/test/java" and "src/test/resources").
   */
  String TEST = "TEST";

}
