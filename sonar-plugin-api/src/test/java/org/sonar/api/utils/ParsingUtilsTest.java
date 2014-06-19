/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.text.ParseException;
import java.util.Locale;

public class ParsingUtilsTest {

  @Test
  public void scaleValue() {
    assertThat(ParsingUtils.scaleValue(23.3333333), is(23.33));
    assertThat(ParsingUtils.scaleValue(23.777777), is(23.78));

    assertThat(ParsingUtils.scaleValue(23.3333333, 0), is(23.0));
    assertThat(ParsingUtils.scaleValue(23.777777, 0), is(24.0));
  }

  @Test
  public void parseString() throws ParseException {
    assertThat(ParsingUtils.parseNumber("23.12", Locale.ENGLISH), is(23.12));
    assertThat(ParsingUtils.parseNumber("12345.67", Locale.ENGLISH), is(12345.67));
    assertThat(ParsingUtils.parseNumber("12345,67", Locale.FRENCH), is(12345.67));
  }
}
