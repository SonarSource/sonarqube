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
package org.sonar.scanner.scan;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.bootstrap.RawScannerProperties;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalProjectKeyAndOrganizationProviderTest {

  private ExternalProjectKeyAndOrganizationLoader loader = mock(ExternalProjectKeyAndOrganizationLoader.class);
  private RawScannerProperties rawProperties = mock(RawScannerProperties.class);

  private ExternalProjectKeyAndOrganizationProvider underTest = new ExternalProjectKeyAndOrganizationProvider();

  @Before
  public void setUp() {
    when(loader.load()).thenReturn(of(new ExternalProjectKeyAndOrganization() {
      @Override
      public Optional<String> getProjectKey() {
        return of("some_key");
      }

      @Override
      public Optional<String> getOrganization() {
        return of("organization_key");
      }
    }));
  }

  @Test
  public void does_nothing_when_key_autodetection_is_disabled() {
    when(rawProperties.property("sonar.keys_autodetection.disabled")).thenReturn("true");

    ExternalProjectKeyAndOrganization result = underTest.provide(rawProperties, loader);

    assertThat(result).isInstanceOf(EmptyExternalProjectKeyAndOrganization.class);
  }

  @Test
  public void by_default_attempts_to_autodetect_keys_if_external_key_loader_detected() {
    when(rawProperties.property("sonar.keys_autodetection.disabled")).thenReturn(null);

    ExternalProjectKeyAndOrganization result = underTest.provide(rawProperties, loader);

    assertThat(result).isNotInstanceOf(EmptyExternalProjectKeyAndOrganization.class);
    assertThat(result.getProjectKey()).isEqualTo(of("some_key"));
    assertThat(result.getOrganization()).isEqualTo(of("organization_key"));
  }

  @Test
  public void by_default_does_nothing_when_no_external_key_loader_detected() {
    when(rawProperties.property("sonar.keys_autodetection.disabled")).thenReturn(null);

    ExternalProjectKeyAndOrganization result = underTest.provide(rawProperties, null);

    assertThat(result).isInstanceOf(EmptyExternalProjectKeyAndOrganization.class);
  }

  @Test
  public void attempts_to_autodetect_keys_if_autodection_is_explicitly_enabled() {
    when(rawProperties.property("sonar.keys_autodetection.disabled")).thenReturn("false");

    ExternalProjectKeyAndOrganization result = underTest.provide(rawProperties, loader);

    assertThat(result).isNotInstanceOf(EmptyExternalProjectKeyAndOrganization.class);
    assertThat(result.getProjectKey()).isEqualTo(of("some_key"));
    assertThat(result.getOrganization()).isEqualTo(of("organization_key"));
  }
}
