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
package org.sonar.server.badge.ws;

import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;


public class ETagUtilsTest {

  @Test
  public void getETag_should_start_with_W_SLASH() {
    assertThat(ETagUtils.getETag(randomAlphanumeric(15))).startsWith("W/");
  }

  @Test
  public void getETag_should_return_same_value_for_same_input() {
    String input = randomAlphanumeric(200);
    assertThat(ETagUtils.getETag(input)).isEqualTo(ETagUtils.getETag(input));
  }
}
