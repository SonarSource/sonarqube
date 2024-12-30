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
package org.sonar.db.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;

class ScrapAnalysisPropertyDtoTest {

  @Test
  void test_empty_value() {
    ScrapAnalysisPropertyDto underTest = new ScrapAnalysisPropertyDto();
    underTest.setEmpty(true);

    assertThat(underTest.getValue()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2000, 4000})
  void test_text_set(int value) {
    ScrapAnalysisPropertyDto underTest = new ScrapAnalysisPropertyDto();
    String text = secure().nextAlphanumeric(value);

    underTest.setTextValue(text);
    assertThat(underTest.getValue()).isEqualTo(text);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2000, 4000})
  void test_clob_set(int value) {
    ScrapAnalysisPropertyDto underTest = new ScrapAnalysisPropertyDto();
    String text = secure().nextAlphanumeric(4000 + value);

    underTest.setClobValue(text);
    assertThat(underTest.getValue()).isEqualTo(text);
  }
}
