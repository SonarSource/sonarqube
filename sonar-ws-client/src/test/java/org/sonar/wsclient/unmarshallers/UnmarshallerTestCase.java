package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.sonar.wsclient.JdkUtils;
import org.sonar.wsclient.services.WSUtils;

import java.io.IOException;

public abstract class UnmarshallerTestCase {

  @BeforeClass
  public static void setupWsUtils() {
    WSUtils.setInstance(new JdkUtils());
  }

  public static String loadFile(String path) {
    try {
      return IOUtils.toString(UnmarshallerTestCase.class.getResourceAsStream(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
