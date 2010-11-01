package org.sonar.java;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;

import com.google.common.collect.Lists;

import java.util.List;

public final class PatternUtils {

  private PatternUtils() {
  }

  public static WildcardPattern[] createMatchers(String pattern) {
    List<WildcardPattern> matchers = Lists.newArrayList();
    if (StringUtils.isNotEmpty(pattern)) {
      String[] patterns = pattern.split(",");
      for (String p : patterns) {
        p = StringUtils.replace(p, ".", "/");
        matchers.add(WildcardPattern.create(p));
      }
    }
    return matchers.toArray(new WildcardPattern[matchers.size()]);
  }

  public static boolean matches(String text, WildcardPattern[] matchers) {
    for (WildcardPattern matcher : matchers) {
      if (matcher.match(text)) {
        return true;
      }
    }
    return false;
  }

}
