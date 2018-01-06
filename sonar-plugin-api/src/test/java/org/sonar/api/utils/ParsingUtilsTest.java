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
package org.sonar.api.utils;

import java.text.ParseException;
import java.util.Locale;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParsingUtilsTest {

  @Test
  public void scaleValue() {
    assertThat(ParsingUtils.scaleValue(23.3333333)).isEqualTo(23.33);
    assertThat(ParsingUtils.scaleValue(23.777777)).isEqualTo(23.78);

    assertThat(ParsingUtils.scaleValue(23.3333333, 0)).isEqualTo(23.0);
    assertThat(ParsingUtils.scaleValue(23.777777, 0)).isEqualTo(24.0);
  }

  @Test
  public void parseString() throws ParseException {
    assertThat(ParsingUtils.parseNumber("23.12", Locale.ENGLISH)).isEqualTo(23.12);
    assertThat(ParsingUtils.parseNumber("12345.67", Locale.ENGLISH)).isEqualTo(12345.67);
    assertThat(ParsingUtils.parseNumber("12345,67", Locale.FRENCH)).isEqualTo(12345.67);
  }
}
