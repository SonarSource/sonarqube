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
package org.sonar.server.issue;

import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;

public class IssueStorage {
  public void insertChanges(IssueChangeMapper mapper, DefaultIssue issue, UuidFactory uuidFactory) {
    for (DefaultIssueComment comment : issue.defaultIssueComments()) {
      if (comment.isNew()) {
        IssueChangeDto changeDto = IssueChangeDto.of(comment, issue.projectUuid());
        changeDto.setUuid(uuidFactory.create());
        changeDto.setProjectUuid(issue.projectUuid());
        mapper.insert(changeDto);
      }
    }
    FieldDiffs diffs = issue.currentChange();
    if (issue.isCopied()) {
      for (FieldDiffs d : issue.changes()) {
        IssueChangeDto changeDto = IssueChangeDto.of(issue.key(), d, issue.projectUuid());
        changeDto.setUuid(uuidFactory.create());
        changeDto.setProjectUuid(issue.projectUuid());
        mapper.insert(changeDto);
      }
    } else if (!issue.isNew() && diffs != null) {
      IssueChangeDto changeDto = IssueChangeDto.of(issue.key(), diffs, issue.projectUuid());
      changeDto.setUuid(uuidFactory.create());
      changeDto.setProjectUuid(issue.projectUuid());
      mapper.insert(changeDto);
    }
  }
}
