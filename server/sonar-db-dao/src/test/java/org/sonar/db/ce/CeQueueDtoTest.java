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

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CeQueueDtoTest {
  private static final String STR_15_CHARS = "012345678901234";
  private static final String STR_40_CHARS = "0123456789012345678901234567890123456789";
  private static final String STR_255_CHARS = STR_40_CHARS + STR_40_CHARS + STR_40_CHARS + STR_40_CHARS
      + STR_40_CHARS + STR_40_CHARS + STR_15_CHARS;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeQueueDto underTest = new CeQueueDto();

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
    expectedException.expectMessage("Value of component UUID is too long: " + str_41_chars);

    underTest.setComponentUuid(str_41_chars);
  }

  @Test
  public void setTaskType_throws_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.setTaskType(null);
  }

  @Test
  public void setTaskType_accepts_empty_and_string_15_chars_or_less() {
    underTest.setTaskType("");
    underTest.setTaskType("bar");
    underTest.setTaskType(STR_15_CHARS);
  }

  @Test
  public void setTaskType_throws_IAE_if_value_is_41_chars() {
    String str_16_chars = STR_15_CHARS + "a";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of task type is too long: " + str_16_chars);

    underTest.setTaskType(str_16_chars);
  }

  @Test
  public void setSubmitterLogin_accepts_null_empty_and_string_255_chars_or_less() {
    underTest.setSubmitterLogin(null);
    underTest.setSubmitterLogin("");
    underTest.setSubmitterLogin("bar");
    underTest.setSubmitterLogin(STR_255_CHARS);
  }

  @Test
  public void setSubmitterLogin_throws_IAE_if_value_is_41_chars() {
    String str_256_chars = STR_255_CHARS + "a";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of submitter login is too long: " + str_256_chars);

    underTest.setSubmitterLogin(str_256_chars);
  }

  @Test
  public void setWorkerUuid_accepts_null_empty_and_string_40_chars_or_less() {
    underTest.setWorkerUuid(null);
    underTest.setWorkerUuid("");
    underTest.setWorkerUuid("bar");
    underTest.setWorkerUuid(STR_40_CHARS);
  }

  @Test
  public void setWorkerUuid_throws_IAE_if_value_is_41_chars() {
    String str_41_chars = STR_40_CHARS + "a";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("worker uuid is too long: " + str_41_chars);

    underTest.setWorkerUuid(str_41_chars);
  }

  @Test
  public void setExecutionCount_throws_IAE_if_value_is_less_than_0() {
    int lessThanZero = -1-(Math.abs(new Random().nextInt()));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("execution count can't be < 0");

    underTest.setExecutionCount(lessThanZero);
  }
}
