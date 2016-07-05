/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.issue.ignore.pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.SonarException;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.issue.ignore.pattern.PatternDecoder;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternDecoderTest {

  private PatternDecoder decoder = new PatternDecoder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldReadString() {
    String patternsList = "# a comment followed by a blank line\n\n" +
      "# suppress all violations\n" +
      "*;*;*\n\n" +
      "# exclude a Java file\n" +
      "com.foo.Bar;*;*\n\n" +
      "# exclude a Java package\n" +
      "com.foo.*;*;*\n\n" +
      "# exclude a specific rule\n" +
      "*;checkstyle:IllegalRegexp;*\n\n" +
      "# exclude a specific rule on a specific file\n" +
      "com.foo.Bar;checkstyle:IllegalRegexp;*\n";
    List<IssuePattern> patterns = decoder.decode(patternsList);

    assertThat(patterns).hasSize(5);
  }

  @Test
  public void shouldCheckFormatOfResource() {
    assertThat(PatternDecoder.isResource("")).isFalse();
    assertThat(PatternDecoder.isResource("*")).isTrue();
    assertThat(PatternDecoder.isResource("com.foo.*")).isTrue();
  }

  @Test
  public void shouldCheckFormatOfRule() {
    assertThat(PatternDecoder.isRule("")).isFalse();
    assertThat(PatternDecoder.isRule("*")).isTrue();
    assertThat(PatternDecoder.isRule("com.foo.*")).isTrue();
  }

  @Test
  public void shouldCheckFormatOfLinesRange() {
    assertThat(PatternDecoder.isLinesRange("")).isFalse();
    assertThat(PatternDecoder.isLinesRange("   ")).isFalse();
    assertThat(PatternDecoder.isLinesRange("12")).isFalse();
    assertThat(PatternDecoder.isLinesRange("12,212")).isFalse();

    assertThat(PatternDecoder.isLinesRange("*")).isTrue();
    assertThat(PatternDecoder.isLinesRange("[]")).isTrue();
    assertThat(PatternDecoder.isLinesRange("[13]")).isTrue();
    assertThat(PatternDecoder.isLinesRange("[13,24]")).isTrue();
    assertThat(PatternDecoder.isLinesRange("[13,24,25-500]")).isTrue();
    assertThat(PatternDecoder.isLinesRange("[24-65]")).isTrue();
    assertThat(PatternDecoder.isLinesRange("[13,24-65,84-89,122]")).isTrue();
  }

  @Test
  public void shouldReadStarPatterns() {
    IssuePattern pattern = decoder.decodeLine("*;*;*");

    assertThat(pattern.getResourcePattern().toString()).isEqualTo("*");
    assertThat(pattern.getRulePattern().toString()).isEqualTo("*");
    assertThat(pattern.isCheckLines()).isFalse();
  }

  @Test
  public void shouldReadLineIds() {
    IssuePattern pattern = decoder.decodeLine("*;*;[10,25,98]");

    assertThat(pattern.isCheckLines()).isTrue();
    assertThat(pattern.getAllLines()).containsOnly(10, 25, 98);
  }

  @Test
  public void shouldReadRangeOfLineIds() {
    IssuePattern pattern = decoder.decodeLine("*;*;[10-12,25,97-100]");

    assertThat(pattern.isCheckLines()).isTrue();
    assertThat(pattern.getAllLines()).containsOnly(10, 11, 12, 25, 97, 98, 99, 100);
  }

  @Test
  public void shouldNotExcludeLines() {
    // [] is different than *
    // - all violations are excluded on *
    // * no violations are excluded on []
    IssuePattern pattern = decoder.decodeLine("*;*;[]");

    assertThat(pattern.isCheckLines()).isTrue();
    assertThat(pattern.getAllLines()).isEmpty();
  }

  @Test
  public void shouldReadBlockPattern() {
    IssuePattern pattern = decoder.decodeLine("SONAR-OFF;SONAR-ON");

    assertThat(pattern.getResourcePattern()).isNull();
    assertThat(pattern.getBeginBlockRegexp()).isEqualTo("SONAR-OFF");
    assertThat(pattern.getEndBlockRegexp()).isEqualTo("SONAR-ON");
  }

  @Test
  public void shouldReadAllFilePattern() {
    IssuePattern pattern = decoder.decodeLine("SONAR-ALL-OFF");

    assertThat(pattern.getResourcePattern()).isNull();
    assertThat(pattern.getAllFileRegexp()).isEqualTo("SONAR-ALL-OFF");
  }

  @Test
  public void shouldFailToReadUncorrectLine1() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Exclusions > Issues : Invalid format. The following line has more than 3 fields separated by comma");

    decoder.decode(";;;;");
  }

  @Test
  public void shouldFailToReadUncorrectLine3() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Exclusions > Issues : Invalid format. The first field does not define a resource pattern");

    decoder.decode(";*;*");
  }

  @Test
  public void shouldFailToReadUncorrectLine4() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Exclusions > Issues : Invalid format. The second field does not define a rule pattern");

    decoder.decode("*;;*");
  }

  @Test
  public void shouldFailToReadUncorrectLine5() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Exclusions > Issues : Invalid format. The third field does not define a range of lines");

    decoder.decode("*;*;blabla");
  }

  @Test
  public void shouldFailToReadUncorrectLine6() {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Exclusions > Issues : Invalid format. The first field does not define a regular expression");

    decoder.decode(";ON");
  }

  @Test
  public void shouldAcceptEmptyEndBlockRegexp() {
    IssuePattern pattern = decoder.decodeLine("OFF;");

    assertThat(pattern.getResourcePattern()).isNull();
    assertThat(pattern.getBeginBlockRegexp()).isEqualTo("OFF");
    assertThat(pattern.getEndBlockRegexp()).isEmpty();
  }
}
