package org.sonar.wsclient.services;

import java.util.Date;

public abstract class WSUtils {

  private static WSUtils INSTANCE = null;

  public static void setInstance(WSUtils utils) {
    INSTANCE = utils;
  }

  public static WSUtils getINSTANCE() {
    return INSTANCE;
  }

  public abstract String format(Date date, String format);

  public abstract String encodeUrl(String url);
}
