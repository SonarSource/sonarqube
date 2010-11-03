package org.sonar.java;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;

public final class PatternUtils {

  private PatternUtils() {
  }

  public static WildcardPattern[] createPatterns(String patterns) {
    return WildcardPattern.create(StringUtils.split(StringUtils.replace(patterns, ".", "/"), ','));
  }

}
