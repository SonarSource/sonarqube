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

import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueComment;

import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ChangeDtoConverterTest {
  @Test
  public void testToChangeDtos() throws Exception {
    IssueComment comment = IssueComment.create("emmerik", "the comment");

    IssueChangeDto dto = ChangeDtoConverter.commentToDto("ABCDE", comment);

    assertThat(dto.getChangeData()).isEqualTo("the comment");
    assertThat(dto.getChangeType()).isEqualTo("comment");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
    assertThat(dto.getIssueKey()).isEqualTo("ABCDE");
    assertThat(dto.getUserLogin()).isEqualTo("emmerik");
  }

  @Test
  public void testToDiffsDtos() throws Exception {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "INFO", "BLOCKER");
    diffs.setUserLogin("emmerik");

    IssueChangeDto dto = ChangeDtoConverter.changeToDto("ABCDE", diffs);

    assertThat(dto.getChangeData()).isEqualTo("severity=INFO|BLOCKER");
    assertThat(dto.getChangeType()).isEqualTo("change");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
    assertThat(dto.getIssueKey()).isEqualTo("ABCDE");
    assertThat(dto.getUserLogin()).isEqualTo("emmerik");
  }
}
