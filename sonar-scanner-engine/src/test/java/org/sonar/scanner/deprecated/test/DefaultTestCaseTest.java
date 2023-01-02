/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.deprecated.test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTestCaseTest {
  private final DefaultTestCase testCase = new DefaultTestCase();

  @Test
  public void getters_after_setters() {
    testCase
      .setName("name")
      .setType("type")
      .setDurationInMs(1234L)
      .setStatus(DefaultTestCase.Status.FAILURE);

    assertThat(testCase.status()).isEqualTo(DefaultTestCase.Status.FAILURE);
    assertThat(testCase.name()).isEqualTo("name");
    assertThat(testCase.type()).isEqualTo("type");
    assertThat(testCase.durationInMs()).isEqualTo(1234L);
  }
}
