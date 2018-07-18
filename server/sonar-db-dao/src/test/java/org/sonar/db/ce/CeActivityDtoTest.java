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
package org.sonar.db.ce;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CeActivityDtoTest {
  private CeActivityDto underTest = new CeActivityDto();

  @Test
  @UseDataProvider("stringsWithChar0")
  public void setStacktrace_filters_out_char_zero(String withChar0, String expected) {
    underTest.setErrorStacktrace(withChar0);

    assertThat(underTest.getErrorStacktrace()).isEqualTo(expected);
  }

  @Test
  @UseDataProvider("stringsWithChar0")
  public void setErrorMessage_filters_out_char_zero(String withChar0, String expected) {
    underTest.setErrorMessage(withChar0);

    assertThat(underTest.getErrorMessage()).isEqualTo(expected);
  }

  @Test
  public void setErrorMessage_truncates_to_1000_after_removing_char_zero() {
    String before = randomAlphanumeric(50);
    String after = randomAlphanumeric(950);
    String truncated = randomAlphanumeric(1 + new Random().nextInt(50));
    underTest.setErrorMessage(before + "\u0000" + after + truncated);

    assertThat(underTest.getErrorMessage()).isEqualTo(before + after);
  }

  @DataProvider
  public static Object[][] stringsWithChar0() {
    return new Object[][] {
      {"\u0000", ""},
      {"\u0000\u0000\u0000\u0000", ""},
      {"\u0000 \u0000a\u0000b\u0000   ", " ab   "},
      {"a \u0000 0message", "a  0message"}
    };
  }
}
