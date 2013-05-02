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
package org.sonar.core.issue.db;

import com.google.common.collect.Lists;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueComment;

import java.util.Date;
import java.util.List;

class ChangeDtoConverter {

  static final String TYPE_FIELD_CHANGE = "diff";
  static final String TYPE_COMMENT = "comment";

  static List<IssueChangeDto> toChangeDtos(DefaultIssue issue) {
    List<IssueChangeDto> dtos = Lists.newArrayList();
    for (IssueComment comment : issue.newComments()) {
      dtos.add(commentToDto(issue.key(), comment));
    }
    if (issue.diffs() != null) {
      dtos.add(diffsToDto(issue.key(), issue.diffs()));
    }
    return dtos;
  }

  static IssueChangeDto commentToDto(String issueKey, IssueComment comment) {
    IssueChangeDto dto = newDto(issueKey);
    dto.setKey(comment.key());
    dto.setChangeType(TYPE_COMMENT);
    dto.setChangeData(comment.text());
    dto.setUserLogin(comment.userLogin());
    return dto;
  }

  static IssueChangeDto diffsToDto(String issueKey, FieldDiffs diffs) {
    IssueChangeDto dto = newDto(issueKey);
    dto.setChangeType(TYPE_FIELD_CHANGE);
    dto.setChangeData(diffs.toString());
    dto.setUserLogin(diffs.userLogin());
    return dto;
  }

  private static IssueChangeDto newDto(String issueKey) {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setIssueKey(issueKey);

    // technical dates - do not use the context date
    Date now = new Date();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(new Date());
    return dto;
  }
}
