/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.almintegration;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.core.util.UuidFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.server.common.almintegration.ProjectKeyGenerator.MAX_PROJECT_KEY_SIZE;
import static org.sonar.server.common.almintegration.ProjectKeyGenerator.PROJECT_KEY_SEPARATOR;

@RunWith(MockitoJUnitRunner.class)
public class ProjectKeyGeneratorTest {

  private static final int MAX_UUID_SIZE = 40;
  private static final String UUID_STRING = RandomStringUtils.secure().nextAlphanumeric(MAX_UUID_SIZE);

  @Mock
  private UuidFactory uuidFactory;

  @InjectMocks
  private ProjectKeyGenerator projectKeyGenerator;

  @Before
  public void setUp() {
    when(uuidFactory.create()).thenReturn(UUID_STRING);
  }

  @Test
  public void generateUniqueProjectKey_shortProjectName_shouldAppendUuid() {
    String fullProjectName = RandomStringUtils.secure().nextAlphanumeric(10);

    assertThat(projectKeyGenerator.generateUniqueProjectKey(fullProjectName))
      .isEqualTo(generateExpectedKeyName(fullProjectName));
  }

  @Test
  public void generateUniqueProjectKey_projectNameEqualsToMaximumSize_shouldTruncateProjectNameAndPreserveUUID() {
    String fullProjectName = RandomStringUtils.secure().nextAlphanumeric(MAX_PROJECT_KEY_SIZE);

    String projectKey = projectKeyGenerator.generateUniqueProjectKey(fullProjectName);
    assertThat(projectKey)
      .hasSize(MAX_PROJECT_KEY_SIZE)
      .isEqualTo(generateExpectedKeyName(fullProjectName.substring(fullProjectName.length() + UUID_STRING.length() + 1 - MAX_PROJECT_KEY_SIZE)));
  }

  @Test
  public void generateUniqueProjectKey_projectNameBiggerThanMaximumSize_shouldTruncateProjectNameAndPreserveUUID() {
    String fullProjectName = RandomStringUtils.secure().nextAlphanumeric(MAX_PROJECT_KEY_SIZE + 50);

    String projectKey = projectKeyGenerator.generateUniqueProjectKey(fullProjectName);
    assertThat(projectKey)
      .hasSize(MAX_PROJECT_KEY_SIZE)
      .isEqualTo(generateExpectedKeyName(fullProjectName.substring(fullProjectName.length() + UUID_STRING.length() + 1 - MAX_PROJECT_KEY_SIZE)));
  }

  @Test
  public void generateUniqueProjectKey_projectNameContainsSlashes_shouldBeEscaped() {
    String fullProjectName = "a/b/c";

    assertThat(projectKeyGenerator.generateUniqueProjectKey(fullProjectName))
      .isEqualTo(generateExpectedKeyName(fullProjectName.replace("/", "_")));
  }

  private String generateExpectedKeyName(String truncatedProjectName) {
    return truncatedProjectName + PROJECT_KEY_SEPARATOR + UUID_STRING;
  }
}
