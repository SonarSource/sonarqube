package org.sonar.batch.bootstrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public enum BootstrapperVersion {

  INSTANCE;

  private static final String PROPERTIES_PATH = "/org/sonar/batch/bootstrapper/version.txt";
  private String version;

  public static String getVersion() {
    return INSTANCE.version;
  }

  private BootstrapperVersion() {
    InputStream input = getClass().getResourceAsStream(PROPERTIES_PATH);
    try {
      Properties properties = new Properties();
      properties.load(input);
      this.version = properties.getProperty("version");

    } catch (IOException e) {
      // Can not load the version
      this.version = "";

    } finally {
      BootstrapperIOUtils.closeQuietly(input);
    }
  }
}
