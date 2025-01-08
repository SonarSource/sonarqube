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
package org.sonar.core.fips;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FipsDetectorTest {

  @ParameterizedTest
  @MethodSource("inputs")
  void testGetters(String[] providerNames, boolean expected) {
    ArrayList<Provider> providers = new ArrayList<>();
    for (String providerName : providerNames) {
      Provider provider = mock(Provider.class);
      when(provider.getName()).thenReturn(providerName);
      providers.add(provider);
    }

    try (MockedStatic<Security> mockedSecurity = mockStatic(Security.class)) {
      mockedSecurity.when(Security::getProviders).thenReturn(providers.toArray(new Provider[0]));

      boolean result = FipsDetector.isFipsEnabled();
      assertThat(result).isEqualTo(expected);
    }
  }

  private static Object[][] inputs() {
    return new Object[][] {
      { new String[]{"FIPS Provider", "SunJSSE", "SunJCE"}, true },
      { new String[]{"Some Provider", "SunJSSE", "SunJCE"}, false }
    };
  }

}
