/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CeActivityDtoTest {
  private static final String STR_40_CHARS = "0123456789012345678901234567890123456789";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeActivityDto underTest = new CeActivityDto();

  @Test
  public void constructor_from_CeQueueDto_populates_fields() {
    long now = new Random().nextLong();
    CeQueueDto ceQueueDto = new CeQueueDto()
      .setUuid(randomAlphanumeric(10))
      .setTaskType(randomAlphanumeric(11))
      .setComponentUuid(randomAlphanumeric(12))
      .setMainComponentUuid(randomAlphanumeric(13))
      .setSubmitterUuid(randomAlphanumeric(14))
      .setWorkerUuid(randomAlphanumeric(15))
      .setCreatedAt(now + 9_999)
      .setStartedAt(now + 865);

    CeActivityDto underTest = new CeActivityDto(ceQueueDto);

    assertThat(underTest.getUuid()).isEqualTo(ceQueueDto.getUuid());
    assertThat(underTest.getTaskType()).isEqualTo(ceQueueDto.getTaskType());
    assertThat(underTest.getComponentUuid()).isEqualTo(ceQueueDto.getComponentUuid());
    assertThat(underTest.getMainComponentUuid()).isEqualTo(ceQueueDto.getMainComponentUuid());
    assertThat(underTest.getIsLastKey()).isEqualTo(ceQueueDto.getTaskType() + ceQueueDto.getComponentUuid());
    assertThat(underTest.getIsLast()).isFalse();
    assertThat(underTest.getMainIsLastKey()).isEqualTo(ceQueueDto.getTaskType() + ceQueueDto.getMainComponentUuid());
    assertThat(underTest.getMainIsLast()).isFalse();
    assertThat(underTest.getSubmitterUuid()).isEqualTo(ceQueueDto.getSubmitterUuid());
    assertThat(underTest.getWorkerUuid()).isEqualTo(ceQueueDto.getWorkerUuid());
    assertThat(underTest.getSubmittedAt()).isEqualTo(ceQueueDto.getCreatedAt());
    assertThat(underTest.getStartedAt()).isEqualTo(ceQueueDto.getStartedAt());
    assertThat(underTest.getStatus()).isNull();
  }

  @Test
  public void setComponentUuid_accepts_null_empty_and_string_40_chars_or_less() {
    underTest.setComponentUuid(null);
    underTest.setComponentUuid("");
    underTest.setComponentUuid("bar");
    underTest.setComponentUuid(STR_40_CHARS);
  }

  @Test
  public void setComponentUuid_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value is too long for column CE_ACTIVITY.COMPONENT_UUID: " + str_41_chars);

    underTest.setComponentUuid(str_41_chars);
  }

  @Test
  public void setMainComponentUuid_accepts_null_empty_and_string_40_chars_or_less() {
    underTest.setMainComponentUuid(null);
    underTest.setMainComponentUuid("");
    underTest.setMainComponentUuid("bar");
    underTest.setMainComponentUuid(STR_40_CHARS);
  }

  @Test
  public void setMainComponentUuid_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value is too long for column CE_ACTIVITY.MAIN_COMPONENT_UUID: " + str_41_chars);

    underTest.setMainComponentUuid(str_41_chars);
  }

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

  @Test
  public void setWarningCount_throws_IAE_if_less_than_0() {
    underTest.setWarningCount(0);
    underTest.setWarningCount(1 + new Random().nextInt(10));

    expectedException.expect(IllegalArgumentException.class);

    underTest.setWarningCount(-1 - new Random().nextInt(10));
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
