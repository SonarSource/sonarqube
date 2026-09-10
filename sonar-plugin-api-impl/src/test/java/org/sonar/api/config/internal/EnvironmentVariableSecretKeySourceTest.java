/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.api.config.internal;

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.config.internal.EnvironmentVariableSecretKeySource.ENVIRONMENT_VARIABLE;

class EnvironmentVariableSecretKeySourceTest {

  private static final String A_BASE64_KEY = "0Vt7v0OTAB2iZlY7CHwLxQ==";

  private final System2 system2 = mock(System2.class);
  private final EnvironmentVariableSecretKeySource underTest = new EnvironmentVariableSecretKeySource(system2);

  @Test
  void loadBase64Key_whenVariableIsSet_shouldReturnIt() {
    when(system2.envVariable(ENVIRONMENT_VARIABLE)).thenReturn(A_BASE64_KEY);

    assertThat(underTest.loadBase64Key()).contains(A_BASE64_KEY);
  }

  @Test
  void loadBase64Key_whenVariableIsSurroundedByWhitespace_shouldTrimIt() {
    when(system2.envVariable(ENVIRONMENT_VARIABLE)).thenReturn("  " + A_BASE64_KEY + "\n");

    assertThat(underTest.loadBase64Key()).contains(A_BASE64_KEY);
  }

  @Test
  void loadBase64Key_whenVariableIsNotSet_shouldReturnEmpty() {
    when(system2.envVariable(ENVIRONMENT_VARIABLE)).thenReturn(null);

    assertThat(underTest.loadBase64Key()).isEmpty();
  }

  @Test
  void loadBase64Key_whenVariableIsBlank_shouldReturnEmpty() {
    when(system2.envVariable(ENVIRONMENT_VARIABLE)).thenReturn("   ");

    assertThat(underTest.loadBase64Key()).isEmpty();
  }

  @Test
  void describe_shouldMentionTheVariableName() {
    assertThat(underTest.describe()).contains(ENVIRONMENT_VARIABLE);
  }
}
