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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServerIdLoaderTest {

  Settings settings = new Settings();
  ServerIdGenerator idGenerator = mock(ServerIdGenerator.class);
  ServerIdLoader underTest = new ServerIdLoader(settings, idGenerator);

  @Test
  public void isValid_returns_true_if_input_equals_newly_generated_id() {
    settings.setProperty(CoreProperties.ORGANISATION, "corp");
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, "1.2.3.4");
    when(idGenerator.generate("corp", "1.2.3.4")).thenReturn("ABC");

    assertThat(underTest.isValid("ABC")).isTrue();
    verify(idGenerator).generate("corp", "1.2.3.4");
  }

  @Test
  public void isValid_returns_false_if_id_cant_be_generated_because_missing_organisation() {
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, "1.2.3.4");

    assertThat(underTest.isValid("ABC")).isFalse();
    verifyZeroInteractions(idGenerator);
  }

  @Test
  public void isValid_returns_false_if_id_cant_be_generated_because_missing_ip() {
    settings.setProperty(CoreProperties.ORGANISATION, "corp");

    assertThat(underTest.isValid("ABC")).isFalse();
    verifyZeroInteractions(idGenerator);
  }

  @Test
  public void isValid_returns_false_if_id_cant_be_generated_because_missing_ip_and_organisation() {
    assertThat(underTest.isValid("ABC")).isFalse();
    verifyZeroInteractions(idGenerator);
  }

  @Test
  public void isValid_returns_false_if_input_different_than_newly_generated_id() {
    settings.setProperty(CoreProperties.ORGANISATION, "corp");
    settings.setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, "1.2.3.4");
    when(idGenerator.generate("corp", "1.2.3.4")).thenReturn("OTHER");

    assertThat(underTest.isValid("ABC")).isFalse();
    verify(idGenerator).generate("corp", "1.2.3.4");
  }

  @Test
  public void get_loads_id_from_settings() {
    assertThat(underTest.get().isPresent()).isFalse();

    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, "ABC");
    assertThat(underTest.get().isPresent()).isTrue();
  }
}
