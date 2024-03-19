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
package org.sonar.server.common.almintegration;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sonar.core.util.UuidFactory;

import static com.google.common.collect.Lists.asList;
import static org.sonar.core.component.ComponentKeys.sanitizeProjectKey;

public class ProjectKeyGenerator {

  @VisibleForTesting
  static final int MAX_PROJECT_KEY_SIZE = 250;
  @VisibleForTesting
  static final Character PROJECT_KEY_SEPARATOR = '_';

  private final UuidFactory uuidFactory;

  public ProjectKeyGenerator(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public String generateUniqueProjectKey(String projectName, String... extraProjectKeyItems) {
    String sqProjectKey = generateCompleteProjectKey(projectName, extraProjectKeyItems);
    sqProjectKey = truncateProjectKeyIfNecessary(sqProjectKey);
    return sanitizeProjectKey(sqProjectKey);
  }

  private String generateCompleteProjectKey(String projectName, String[] extraProjectKeyItems) {
    List<String> projectKeyItems = asList(projectName, extraProjectKeyItems);
    String projectKey = StringUtils.join(projectKeyItems, PROJECT_KEY_SEPARATOR);
    String uuid = uuidFactory.create();
    return projectKey + PROJECT_KEY_SEPARATOR + uuid;
  }

  private static String truncateProjectKeyIfNecessary(String sqProjectKey) {
    if (sqProjectKey.length() > MAX_PROJECT_KEY_SIZE) {
      return sqProjectKey.substring(sqProjectKey.length() - MAX_PROJECT_KEY_SIZE);
    }
    return sqProjectKey;
  }

}
