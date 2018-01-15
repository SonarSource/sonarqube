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
package org.sonar.server.sticker.ws;

import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.sticker.ws.NumberFormatter.formatNumber;

public class NumberFormatterTest {

  @Test
  public void format_zero() {
    assertThat(formatNumber(0L)).isEqualTo("0");
  }

  @Test
  public void format_less_than_thousand() {
    assertThat(formatNumber(5L)).isEqualTo("5");
    assertThat(formatNumber(950L)).isEqualTo("950");
  }

  @Test
  public void format_thousands() {
    assertThat(formatNumber(1000L)).isEqualTo("1k");
    assertThat(formatNumber(1010L)).isEqualTo("1k");
    assertThat(formatNumber(1100L)).isEqualTo("1.1k");
    assertThat(formatNumber(1690L)).isEqualTo("1.7k");
    assertThat(formatNumber(950000L)).isEqualTo("950k");
  }

  @Test
  public void format_millions() {
    assertThat(formatNumber(1000000L)).isEqualTo("1m");
    assertThat(formatNumber(1010000L)).isEqualTo("1m");
  }

  @Test
  public void format_billions() {
    assertThat(formatNumber(1000000000L)).isEqualTo("1b");
  }

  @Test
  public void format_terras() {
    assertThat(formatNumber(1000000000000L)).isEqualTo("1t");
  }

  @Test
  public void only_statics() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(NumberFormatter.class)).isTrue();
  }
}
