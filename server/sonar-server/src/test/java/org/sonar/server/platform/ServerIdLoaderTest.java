/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform;

import java.util.Optional;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServerIdLoaderTest {

  private static final String AN_ID = "ABC";
  private static final String AN_IP = "1.2.3.4";
  public static final String AN_ORGANIZATION = "corp";

  MapSettings settings = new MapSettings();
  ServerIdGenerator idGenerator = mock(ServerIdGenerator.class);
  ServerIdLoader underTest = new ServerIdLoader(settings.asConfig(), idGenerator);

  @Test
  public void get_returns_absent_if_id_property_is_not_set() {
    settings.setProperty(CoreProperties.ORGANISATION, AN_ORGANIZATION);
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, AN_IP);

    Optional<ServerId> serverIdOpt = underTest.get();
    assertThat(serverIdOpt).isEmpty();
    verifyZeroInteractions(idGenerator);
  }

  @Test
  public void get_returns_valid_id() {
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, AN_ID);
    settings.setProperty(CoreProperties.ORGANISATION, AN_ORGANIZATION);
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, AN_IP);
    when(idGenerator.validate(AN_ORGANIZATION, AN_IP, AN_ID)).thenReturn(true);

    Optional<ServerId> serverIdOpt = underTest.get();
    verifyServerId(serverIdOpt.get(), AN_ID, true);
    verify(idGenerator).validate(AN_ORGANIZATION, AN_IP, AN_ID);
  }

  @Test
  public void get_returns_invalid_id_if_id_cant_be_generated_because_missing_organization() {
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, AN_ID);
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, AN_IP);

    Optional<ServerId> serverIdOpt = underTest.get();

    verifyServerId(serverIdOpt.get(), AN_ID, false);
  }

  @Test
  public void get_returns_invalid_id_if_id_cant_be_generated_because_missing_ip() {
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, AN_ID);
    settings.setProperty(CoreProperties.ORGANISATION, AN_ORGANIZATION);

    Optional<ServerId> serverIdOpt = underTest.get();

    verifyServerId(serverIdOpt.get(), AN_ID, false);
    verifyZeroInteractions(idGenerator);
  }

  @Test
  public void get_returns_invalid_id_if_id_cant_be_generated_because_missing_ip_and_organization() {
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, AN_ID);

    Optional<ServerId> serverIdOpt = underTest.get();

    verifyServerId(serverIdOpt.get(), AN_ID, false);
    verifyZeroInteractions(idGenerator);
  }

  @Test
  public void get_returns_invalid_id_if_input_is_different_than_newly_generated_id() {
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, AN_ID);
    settings.setProperty(CoreProperties.ORGANISATION, AN_ORGANIZATION);
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, AN_IP);
    when(idGenerator.generate(AN_ORGANIZATION, AN_IP)).thenReturn("OTHER");

    Optional<ServerId> serverIdOpt = underTest.get();

    verifyServerId(serverIdOpt.get(), AN_ID, false);
    verify(idGenerator).validate(AN_ORGANIZATION, AN_IP, AN_ID);
  }

  @Test
  public void getRaw_loads_id_from_settings() {
    assertThat(underTest.getRaw().isPresent()).isFalse();

    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, AN_ID);
    assertThat(underTest.getRaw().isPresent()).isTrue();
  }

  private static void verifyServerId(ServerId serverId, String expectedId, boolean expectedValid) {
    assertThat(serverId.getId()).isEqualTo(expectedId);
    assertThat(serverId.isValid()).isEqualTo(expectedValid);
  }
}
