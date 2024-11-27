/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.ai.code.assurance;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCodeAssuranceEntitlementTest {
  private final PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);

  @ParameterizedTest
  @MethodSource("isEnabledParams")
  void isEnabled(EditionProvider.Edition edition, boolean expected) {
    when(platformEditionProvider.get()).thenReturn(Optional.of(edition));
    AiCodeAssuranceEntitlement underTest = new AiCodeAssuranceEntitlement(platformEditionProvider);

    assertThat(underTest.isEnabled()).isEqualTo(expected);
  }

  private static Stream<Arguments> isEnabledParams() {
    return Stream.of(
      Arguments.of(EditionProvider.Edition.COMMUNITY, false),
      Arguments.of(EditionProvider.Edition.DEVELOPER, true),
      Arguments.of(EditionProvider.Edition.ENTERPRISE, true),
      Arguments.of(EditionProvider.Edition.DATACENTER, true)
    );
  }
}
