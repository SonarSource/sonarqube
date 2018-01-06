/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternDecoderTest {
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
}
