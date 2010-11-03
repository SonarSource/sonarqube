package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;

public final class PluginKeyUtils {

  public static String getPluginKey(String pluginKey) {
    String key = pluginKey;
    if (StringUtils.startsWith(pluginKey, "sonar-") && StringUtils.endsWith(pluginKey, "-plugin")) {
      key = StringUtils.removeEnd(StringUtils.removeStart(pluginKey, "sonar-"), "-plugin");
    } else if (StringUtils.endsWith(pluginKey, "-sonar-plugin")) {
      key = StringUtils.removeEnd(pluginKey, "-sonar-plugin");
    }
    return StringUtils.remove(key, "-");
  }

  private PluginKeyUtils() {
  }

}
