/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.issue.condition;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @since 3.6
 */
public final class HasIssuePropertyCondition implements Condition {

  private final String propertyKey;

  public HasIssuePropertyCondition(String propertyKey) {
    Preconditions.checkArgument(!isEmpty(propertyKey));
    this.propertyKey = propertyKey;
  }

  public String getPropertyKey() {
    return propertyKey;
  }

  @Override
  public boolean matches(Issue issue) {
    return !StringUtils.isEmpty(issue.attribute(propertyKey));
  }
}
