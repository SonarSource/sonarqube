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
package org.sonar.db.component;

import java.util.Random;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class ScrapAnalysisPropertyDtoTest {

  private Random random = new Random();

  @Test
  public void test_empty_value() {
    ScrapAnalysisPropertyDto underTest = new ScrapAnalysisPropertyDto();
    underTest.setEmpty(true);

    assertThat(underTest.getValue()).isEqualTo("");
  }

  @Test
  public void test_text_set() {
    ScrapAnalysisPropertyDto underTest = new ScrapAnalysisPropertyDto();
    String value = randomAlphanumeric(random.nextInt(4000));
    underTest.setTextValue(value);

    assertThat(underTest.getValue()).isEqualTo(value);
  }

  @Test
  public void test_clob_set() {
    ScrapAnalysisPropertyDto underTest = new ScrapAnalysisPropertyDto();
    String value = randomAlphanumeric(4000 + random.nextInt(4000));
    underTest.setClobValue(value);

    assertThat(underTest.getValue()).isEqualTo(value);
  }
}
