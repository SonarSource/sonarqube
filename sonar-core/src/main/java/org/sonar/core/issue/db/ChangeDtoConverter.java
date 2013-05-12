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

import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;

import java.util.Date;

class ChangeDtoConverter {

  private ChangeDtoConverter() {
    // only static methods
  }

  static final String TYPE_FIELD_CHANGE = "diff";
  static final String TYPE_COMMENT = "comment";

  static IssueChangeDto commentToDto(DefaultIssueComment comment) {
    IssueChangeDto dto = newDto(comment.issueKey());
    dto.setKey(comment.key());
    dto.setChangeType(TYPE_COMMENT);
    dto.setChangeData(comment.text());
    dto.setUserLogin(comment.userLogin());
    return dto;
  }

  static IssueChangeDto changeToDto(String issueKey, FieldDiffs diffs) {
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

  public static DefaultIssueComment dtoToComment(IssueChangeDto dto) {
    return new DefaultIssueComment()
      .setText(dto.getChangeData())
      .setKey(dto.getKey())
      .setCreatedAt(dto.getCreatedAt())
      .setUpdatedAt(dto.getUpdatedAt())
      .setUserLogin(dto.getUserLogin())
      .setIssueKey(dto.getIssueKey())
      .setNew(false);
  }

  public static FieldDiffs dtoToChange(IssueChangeDto dto) {
    FieldDiffs diffs = FieldDiffs.parse(dto.getChangeData());
    diffs.setUserLogin(dto.getUserLogin());
    diffs.setCreatedAt(dto.getCreatedAt());
    diffs.setUpdatedAt(dto.getUpdatedAt());
    return diffs;
  }
}
