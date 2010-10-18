package org.sonar.plugins.findbugs;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public enum FindbugsVersion {
  INSTANCE;

  private static final String PROPERTIES_PATH = "/org/sonar/plugins/findbugs/findbugs-plugin.properties";
  private String version;

  public static String getVersion() {
    return INSTANCE.version;
  }

  private FindbugsVersion() {
    InputStream input = getClass().getResourceAsStream(PROPERTIES_PATH);
    try {
      Properties properties = new Properties();
      properties.load(input);
      this.version = properties.getProperty("findbugs.version");

    } catch (IOException e) {
      LoggerFactory.getLogger(getClass()).warn("Can not load the Findbugs version from the file " + PROPERTIES_PATH);
      this.version = "";

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
