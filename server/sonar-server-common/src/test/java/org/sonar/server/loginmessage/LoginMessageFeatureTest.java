/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.loginmessage;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class LoginMessageFeatureTest {

  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final LoginMessageFeature underTest = new LoginMessageFeature(sonarRuntime);

  @Test
  @UseDataProvider("editionsAndLoginMessageFeatureAvailability")
  public void isAvailable_shouldOnlyBeEnabledInEnterpriseEditionPlus(SonarEdition edition, boolean shouldBeEnabled) {
    when(sonarRuntime.getEdition()).thenReturn(edition);

    boolean isAvailable = underTest.isAvailable();

    assertThat(isAvailable).isEqualTo(shouldBeEnabled);
  }

  @Test
  public void getName_ShouldReturn_RegulatoryReports() {
    assertEquals("login-message", underTest.getName());
  }

  @DataProvider
  public static Object[][] editionsAndLoginMessageFeatureAvailability() {
    return new Object[][] {
      {SonarEdition.COMMUNITY, false},
      {SonarEdition.DEVELOPER, false},
      {SonarEdition.ENTERPRISE, true},
      {SonarEdition.DATACENTER, true}
    };
  }
}
