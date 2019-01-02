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

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskMessageDtoTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTaskMessageDto underTest = new CeTaskMessageDto();

  @Test
  public void setMessage_fails_with_IAE_if_argument_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("message can't be null nor empty");

    underTest.setMessage(null);
  }

  @Test
  public void setMessage_fails_with_IAE_if_argument_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("message can't be null nor empty");

    underTest.setMessage("");
  }

  @Test
  public void setMessage_accept_argument_of_size_4000() {
    String str = repeat("a", 4000);
    underTest.setMessage(str);

    assertThat(underTest.getMessage()).isEqualTo(str);
  }

  @Test
  public void setMessage_fails_with_IAE_if_argument_has_size_bigger_then_4000() {
    int size = 4000 + 1 + new Random().nextInt(100);
    String str = repeat("a", size);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("message is too long: " + size);

    underTest.setMessage(str);
  }
}
