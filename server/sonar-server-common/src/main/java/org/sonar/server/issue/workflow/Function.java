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
package org.sonar.server.issue.workflow;

import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.db.user.UserDto;

interface Function {
  interface Context {
    Issue issue();

    Context setAssignee(@Nullable UserDto user);

    Context setResolution(@Nullable String s);

    Context setCloseDate();

    Context unsetCloseDate();

    Context unsetLine();

    Context setType(@Nullable RuleType type);
  }

  void execute(Context context);
}
