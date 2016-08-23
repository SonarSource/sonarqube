/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.setting;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_KEY;

public class SettingsServiceTest {

  @Rule
  public ServiceTester<SettingsService> serviceTester = new ServiceTester<>(new SettingsService(mock(WsConnector.class)));

  private SettingsService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void list_definitions() {
    ListDefinitionsRequest request = ListDefinitionsRequest.builder()
      .setComponentKey("KEY")
      .build();

    underTest.listDefinitions(request);
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(Settings.ListDefinitionsWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_COMPONENT_KEY, "KEY")
      .andNoOtherParam();
  }

}
