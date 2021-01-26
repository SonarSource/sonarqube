/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class TimeoutConfigurationImplTest {
  private final MapSettings settings = new MapSettings();
  private final TimeoutConfigurationImpl underTest = new TimeoutConfigurationImpl(settings.asConfig());

  @Test
  public void getConnectTimeout_returns_10000_when_property_is_not_defined() {
    assertThat(underTest.getConnectTimeout()).isEqualTo(30_000L);
  }

  @Test
  @UseDataProvider("notALongPropertyValues")
  public void getConnectTimeout_returns_10000_when_property_is_not_a_long(String notALong) {
    settings.setProperty("sonar.alm.timeout.connect", notALong);

    assertThat(underTest.getConnectTimeout()).isEqualTo(30_000L);
  }

  @Test
  public void getConnectTimeout_returns_value_of_property() {
    long expected = new Random().nextInt(9_456_789);
    settings.setProperty("sonar.alm.timeout.connect", expected);

    assertThat(underTest.getConnectTimeout()).isEqualTo(expected);
  }

  @Test
  public void getReadTimeout_returns_10000_when_property_is_not_defined() {
    assertThat(underTest.getReadTimeout()).isEqualTo(30_000L);
  }

  @Test
  @UseDataProvider("notALongPropertyValues")
  public void getReadTimeout_returns_10000_when_property_is_not_a_long(String notALong) {
    settings.setProperty("sonar.alm.timeout.read", notALong);

    assertThat(underTest.getReadTimeout()).isEqualTo(30_000L);
  }

  @Test
  public void getReadTimeout_returns_value_of_property() {
    long expected = new Random().nextInt(9_456_789);
    settings.setProperty("sonar.alm.timeout.read", expected);

    assertThat(underTest.getReadTimeout()).isEqualTo(expected);
  }

  @DataProvider
  public static Object[][] notALongPropertyValues() {
    return new Object[][] {
      {"foo"},
      {""},
      {"12.5"}
    };
  }
}
