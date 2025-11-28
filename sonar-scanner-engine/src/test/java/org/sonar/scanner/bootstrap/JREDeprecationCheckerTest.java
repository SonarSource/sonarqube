/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JREDeprecationCheckerTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Mock
  private AnalysisWarnings analysisWarnings;

  @Mock
  private System2 system2;

  @Test
  void should_add_warning_when_java_version_is_17() {
    when(system2.property("java.version")).thenReturn("17.0.9");

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    var expectedMessage = """
      Java 17 scanner support ends with SonarQube 2026.3 (July 2026). \
      Please upgrade to Java 21 or newer, or use JRE auto-provisioning to keep this requirement always up to date.""";
    verify(analysisWarnings).addUnique(expectedMessage);
    assertThat(logTester.logs(Level.WARN)).containsOnly(expectedMessage);
  }

  @Test
  void should_add_warning_when_java_version_is_20() {
    when(system2.property("java.version")).thenReturn("20");

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    var expectedMessage = """
      Java 20 scanner support ends with SonarQube 2026.3 (July 2026). \
      Please upgrade to Java 21 or newer, or use JRE auto-provisioning to keep this requirement always up to date.""";
    verify(analysisWarnings).addUnique(expectedMessage);
    assertThat(logTester.logs(Level.WARN)).containsOnly(expectedMessage);
  }

  @Test
  void should_not_add_warning_when_java_version_is_21() {
    when(system2.property("java.version")).thenReturn("21.0.9");

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    verify(analysisWarnings, never()).addUnique(anyString());
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_not_add_warning_when_java_version_is_23() {
    when(system2.property("java.version")).thenReturn("23.0.1");

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    verify(analysisWarnings, never()).addUnique(anyString());
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_not_add_warning_when_java_version_is_null() {
    when(system2.property("java.version")).thenReturn(null);

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    verify(analysisWarnings, never()).addUnique(anyString());
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_not_add_warning_when_java_version_is_malformed() {
    when(system2.property("java.version")).thenReturn("invalid-version");

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    verify(analysisWarnings, never()).addUnique(anyString());
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_not_add_warning_when_java_version_has_no_numeric_part() {
    when(system2.property("java.version")).thenReturn("abc.def.ghi");

    var underTest = new JREDeprecationChecker(analysisWarnings, system2);
    underTest.start();

    verify(analysisWarnings, never()).addUnique(anyString());
    assertThat(logTester.logs()).isEmpty();
  }
}
