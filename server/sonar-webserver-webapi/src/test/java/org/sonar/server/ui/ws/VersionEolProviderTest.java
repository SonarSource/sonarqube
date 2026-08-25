/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.ui.ws;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.utils.System2;
import org.sonar.server.platform.ws.SupportType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class VersionEolProviderTest {

  private static final String STANDARD_EOL = "2025-01-15";
  private static final String PREMIUM_EOL = "2026-07-15";

  @Test
  void getEffectiveVersionEol_returns_premium_eol_when_lta_and_premium_support() {
    try (MockedStatic<MetadataLoader> loader = mockStatic(MetadataLoader.class)) {
      loader.when(() -> MetadataLoader.loadSqIsLta(System2.INSTANCE)).thenReturn(true);
      loader.when(() -> MetadataLoader.loadSqVersionEol(System2.INSTANCE)).thenReturn(STANDARD_EOL);
      loader.when(() -> MetadataLoader.loadSqPremiumVersionEol(System2.INSTANCE)).thenReturn(PREMIUM_EOL);

      VersionEolProvider provider = new VersionEolProvider(() -> SupportType.PREMIUM);

      assertThat(provider.getEffectiveVersionEol()).isEqualTo(PREMIUM_EOL);
    }
  }

  @Test
  void getEffectiveVersionEol_returns_standard_eol_when_lta_and_standard_support() {
    try (MockedStatic<MetadataLoader> loader = mockStatic(MetadataLoader.class)) {
      loader.when(() -> MetadataLoader.loadSqIsLta(System2.INSTANCE)).thenReturn(true);
      loader.when(() -> MetadataLoader.loadSqVersionEol(System2.INSTANCE)).thenReturn(STANDARD_EOL);

      VersionEolProvider provider = new VersionEolProvider(() -> SupportType.STANDARD);

      assertThat(provider.getEffectiveVersionEol()).isEqualTo(STANDARD_EOL);
    }
  }

  @Test
  void getEffectiveVersionEol_returns_standard_eol_when_not_lta_even_with_premium_support() {
    try (MockedStatic<MetadataLoader> loader = mockStatic(MetadataLoader.class)) {
      loader.when(() -> MetadataLoader.loadSqIsLta(System2.INSTANCE)).thenReturn(false);
      loader.when(() -> MetadataLoader.loadSqVersionEol(System2.INSTANCE)).thenReturn(STANDARD_EOL);

      VersionEolProvider provider = new VersionEolProvider(() -> SupportType.PREMIUM);

      assertThat(provider.getEffectiveVersionEol()).isEqualTo(STANDARD_EOL);
    }
  }

  @Test
  void getEffectiveVersionEol_returns_standard_eol_when_no_license() {
    try (MockedStatic<MetadataLoader> loader = mockStatic(MetadataLoader.class)) {
      loader.when(() -> MetadataLoader.loadSqIsLta(System2.INSTANCE)).thenReturn(true);
      loader.when(() -> MetadataLoader.loadSqVersionEol(System2.INSTANCE)).thenReturn(STANDARD_EOL);

      VersionEolProvider provider = new VersionEolProvider(() -> null);

      assertThat(provider.getEffectiveVersionEol()).isEqualTo(STANDARD_EOL);
    }
  }
}
