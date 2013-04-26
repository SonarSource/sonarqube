/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.BatchExtension;

import javax.annotation.Nullable;

/**
 * @since 3.6
 */
public interface IssueHandler extends BatchExtension {

  interface IssueContext {
    Issue issue();

    boolean isNew();

    boolean isAlive();

    IssueContext setLine(@Nullable Integer line);

    IssueContext setDescription(String description);

    // set manual severity ?
    IssueContext setSeverity(String severity);

    // TODO rename to setScmLogin ?
    IssueContext setAuthorLogin(@Nullable String login);

    IssueContext setAttribute(String key, @Nullable String value);

    IssueContext assignTo(@Nullable String login);

    //TODO IssueContext comment(String comment);

  }

  void onIssue(IssueContext context);

}
