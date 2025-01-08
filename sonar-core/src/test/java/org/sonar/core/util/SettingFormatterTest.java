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
package org.sonar.core.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingFormatterTest {

  @Test
  public void fromJavaPropertyToEnvVariable() {
    String output = SettingFormatter.fromJavaPropertyToEnvVariable("some.randomProperty-123.test");
    assertThat(output).isEqualTo("SOME_RANDOMPROPERTY_123_TEST");
  }

  @Test
  public void test_getStringArrayBySeparator_on_input_with_separator() {
    String[] result = SettingFormatter.getStringArrayBySeparator(" abc, DeF  , ghi", ",");
    assertThat(result).containsExactly("abc", "DeF", "ghi");
  }

  @Test
  public void test_getStringArrayBySeparator_on_input_without_separator() {
    String[] result = SettingFormatter.getStringArrayBySeparator(" abc, DeF  , ghi", ";");
    assertThat(result).containsExactly("abc, DeF  , ghi");
  }
}
