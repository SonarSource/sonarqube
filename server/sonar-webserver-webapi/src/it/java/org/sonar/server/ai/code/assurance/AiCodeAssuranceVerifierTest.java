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
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCodeAssuranceVerifierTest {
  private final ProjectDto projectDto = mock(ProjectDto.class);
  private final PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);
  private AiCodeAssuranceVerifier underTest;

  public static Stream<Arguments> provideParams() {
    return Stream.of(
      Arguments.of(Edition.COMMUNITY, true, false),
      Arguments.of(Edition.COMMUNITY, false, false),
      Arguments.of(Edition.DEVELOPER, true, true),
      Arguments.of(Edition.DEVELOPER, false, false),
      Arguments.of(Edition.ENTERPRISE, true, true),
      Arguments.of(Edition.ENTERPRISE, false, false),
      Arguments.of(Edition.DATACENTER, true, true),
      Arguments.of(Edition.DATACENTER, false, false)
    );
  }

  @ParameterizedTest
  @MethodSource("provideParams")
  void isAiCodeAssured(Edition edition, boolean aiCodeAssuredOnProject, boolean expected) {
    when(platformEditionProvider.get()).thenReturn(Optional.of(edition));
    underTest = new AiCodeAssuranceVerifier(platformEditionProvider);

    when(projectDto.getContainsAiCode()).thenReturn(aiCodeAssuredOnProject);

    assertThat(underTest.isAiCodeAssured(projectDto.getContainsAiCode())).isEqualTo(expected);
    assertThat(underTest.isAiCodeAssured(projectDto)).isEqualTo(expected);
  }
}
