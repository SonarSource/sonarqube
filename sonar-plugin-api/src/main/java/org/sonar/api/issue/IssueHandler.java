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
package org.sonar.api.issue;

import org.sonar.api.BatchSide;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.user.User;

import javax.annotation.Nullable;

/**
 * @since 3.6
 */
@BatchSide
@ExtensionPoint
public interface IssueHandler {

  interface Context {
    Issue issue();

    boolean isNew();

    boolean isEndOfLife();

    Context setLine(@Nullable Integer i);

    Context setMessage(@Nullable String s);

    Context setSeverity(String s);

    Context setEffortToFix(@Nullable Double d);

    Context setAuthorLogin(@Nullable String s);

    Context setAttribute(String key, @Nullable String value);

    /**
     * @deprecated since 3.7.1
     */
    @Deprecated
    Context assign(@Nullable String login);

    Context assign(@Nullable User user);

    Context addComment(String text);

  }

  void onIssue(Context context);

}
