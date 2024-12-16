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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoOpAiCodeAssuranceVerifierTest {
  private static final String BRANCH_KEY = "branchKey";
  private final ProjectDto projectDto = mock(ProjectDto.class);
  private AiCodeAssuranceVerifier underTest;

  @BeforeEach
  void setUp() {
    underTest = new NoOpAiCodeAssuranceVerifier();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isAiCodeAssuredForProject(boolean containsAiCode) {
    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);

    assertThat(underTest.isAiCodeAssured(projectDto)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void getAiCodeAssuranceForProject(boolean containsAiCode) {
    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);

    assertThat(underTest.getAiCodeAssurance(projectDto)).isEqualTo(AiCodeAssurance.NONE);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void getAiCodeAssuranceForBranch(boolean containsAiCode) {
    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);

    assertThat(underTest.getAiCodeAssurance(projectDto, BRANCH_KEY)).isEqualTo(AiCodeAssurance.NONE);
  }
}
