/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.api.server.debt;

import javax.annotation.CheckForNull;

/**
 * @since 4.3
 * @deprecated in 5.2. It will be dropped in version 6.0 (see https://jira.sonarsource.com/browse/SONAR-6393)
 */
@Deprecated
public interface DebtCharacteristic {

  /**
   * Only used when a characteristic is disabled (id is -1 in dto) by the user. see {@link org.sonar.server.rule.index.RuleNormalizer}
   */
  String NONE = "NONE";

  String key();

  String name();

  @CheckForNull
  Integer order();

  boolean isSub();
}
