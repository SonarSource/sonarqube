/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.core.component;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ComponentKeysSanitizationTest {

  @Parameterized.Parameters(name = "{index}: input {0}, expected output {1}")
  public static Collection<String[]> data() {
    return Arrays.asList(new String[][] {
      {"/a/b/c/", "_a_b_c_"},
      {".a.b:c:", ".a.b:c:"},
      {"_1_2_3_", "_1_2_3_"},
      {"fully_valid_-name2", "fully_valid_-name2"},
      {"°+\"*ç%&\\/()=?`^“#Ç[]|{}≠¿ ~", "___________________________"},
    });
  }

  private final String inputString;
  private final String expectedOutputString;

  public ComponentKeysSanitizationTest(String inputString, String expectedOutputString) {
    this.inputString = inputString;
    this.expectedOutputString = expectedOutputString;
  }

  @Test
  public void sanitizeProjectKey() {
    assertThat(ComponentKeys.sanitizeProjectKey(inputString)).isEqualTo(expectedOutputString);
  }

}
