/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import static org.junit.Assert.assertThat;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindbugsAntConverterTest {

  @Test
  public void convertToJavaRegexFormat() {
    assertAntPatternEqualsToFindBugsRegExp("foo", "~foo", "foo");
    assertAntPatternEqualsToFindBugsRegExp("**/*Test.java", "~(.*\\.)?[^\\\\^\\s]*Test", "Test");
    assertAntPatternEqualsToFindBugsRegExp("**/*Test.java", "~(.*\\.)?[^\\\\^\\s]*Test", "foo.bar.Test");
  }

  @Test
  public void shouldConvertAntToJavaRegexp() {
    // see SONAR-853
    assertAntPatternEqualsToFindBugsRegExp("?", "~.", "g");
    assertAntPatternEqualsToFindBugsRegExp("*/myClass.JaVa", "~([^\\\\^\\s]*\\.)?myClass", "foo.bar.test.myClass");
    assertAntPatternEqualsToFindBugsRegExp("*/myClass.java", "~([^\\\\^\\s]*\\.)?myClass", "foo.bar.test.myClass");
    assertAntPatternEqualsToFindBugsRegExp("*/myClass2.jav", "~([^\\\\^\\s]*\\.)?myClass2", "foo.bar.test.myClass2");
    assertAntPatternEqualsToFindBugsRegExp("*/myOtherClass", "~([^\\\\^\\s]*\\.)?myOtherClass", "foo.bar.test.myOtherClass");
    assertAntPatternEqualsToFindBugsRegExp("*", "~[^\\\\^\\s]*", "ga.%#123_(*");
    assertAntPatternEqualsToFindBugsRegExp("**", "~.*", "gd.3reqg.3151];9#@!");
    assertAntPatternEqualsToFindBugsRegExp("**/generated/**", "~(.*\\.)?generated\\..*", "!@$Rq/32T$).generated.##TR.e#@!$");
    assertAntPatternEqualsToFindBugsRegExp("**/cl*nt/*", "~(.*\\.)?cl[^\\\\^\\s]*nt\\.[^\\\\^\\s]*", "!#$_.clr31r#!$(nt.!#$QRW)(.");
    assertAntPatternEqualsToFindBugsRegExp("**/org/apache/commons/**", "~(.*\\.)?org\\.apache\\.commons\\..*", "org.apache.commons.httpclient.contrib.ssl");
    assertAntPatternEqualsToFindBugsRegExp("*/org/apache/commons/**", "~([^\\\\^\\s]*\\.)?org\\.apache\\.commons\\..*", "org.apache.commons.httpclient.contrib.ssl");
    assertAntPatternEqualsToFindBugsRegExp("org/apache/commons/**", "~org\\.apache\\.commons\\..*", "org.apache.commons.httpclient.contrib.ssl");
  }

  @Test
  public void shouldntMatchThoseClassPattern() {
    // see SONAR-853
    assertJavaRegexpResult("[^\\\\^\\s]", "fad f.ate 12#)", false);
  }

  private void assertAntPatternEqualsToFindBugsRegExp(String antPattern, String regExp, String example) {
    assertThat(FindbugsAntConverter.antToJavaRegexpConvertor(antPattern), Is.is(regExp));
    String javaRegexp = regExp.substring(1, regExp.length());
    assertJavaRegexpResult(javaRegexp, example, true);
  }

  private void assertJavaRegexpResult(String javaRegexp, String example, boolean expectedResult) {
    Pattern pattern = Pattern.compile(javaRegexp);
    Matcher matcher = pattern.matcher(example);
    assertThat(example + " tested with pattern " + javaRegexp, matcher.matches(), Is.is(expectedResult));
  }

}
