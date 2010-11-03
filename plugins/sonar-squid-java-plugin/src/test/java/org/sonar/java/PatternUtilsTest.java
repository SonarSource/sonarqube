package org.sonar.java;

import org.junit.Test;
import org.sonar.api.utils.WildcardPattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PatternUtilsTest {

  @Test
  public void shouldConvertJavaPackagesToPatterns() {
    WildcardPattern[] patterns = PatternUtils.createPatterns("org.sonar.Foo,javax.**");

    assertThat(patterns.length, is(2));
    assertThat(patterns[0].match("org/sonar/Foo"), is(true));
    assertThat(patterns[1].match("javax.Bar"), is(true));
  }
}
