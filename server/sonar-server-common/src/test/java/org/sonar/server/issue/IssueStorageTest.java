/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Date;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssueStorageTest {
  private final IssueStorage underTest = new IssueStorage();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final IssueChangeMapper issueChangeMapper = mock(IssueChangeMapper.class);

  @Test
  public void when_newIssueWithAnticipatedTransitionInserted_twoChangelogCreated() {
    when(uuidFactory.create()).thenReturn("uuid");

    String issueKey = "ABCDE";
    String commentText = "comment for new issue";
    DefaultIssueComment comment = DefaultIssueComment.create(issueKey, "user_uuid", commentText);
    comment.setKey("FGHIJ");
    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");

    DefaultIssue issue = new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.BUG)
      .setNew(true)
      .setRuleKey(RuleKey.of("keyRepo", "r:2145"))
      .setProjectUuid("projectUuid")
      .setComponentUuid("fileUuid")
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .setResolution("wontfix")
      .setStatus("CLOSED")
      .setSeverity("BLOCKER")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)
      .setCurrentChange(new FieldDiffs())
      .setAnticipatedTransitionUuid("anticipatedTransitionUuid");

    IssueChangeDto mockCreated = mock(IssueChangeDto.class);
    IssueChangeDto mockAnticipatedTransition = mock(IssueChangeDto.class);
    try (MockedStatic<IssueChangeDto> issueChangeDtoMockedStatic = mockStatic(IssueChangeDto.class)) {
      issueChangeDtoMockedStatic.when(() -> IssueChangeDto.of(any(DefaultIssueComment.class), anyString()))
        .thenReturn(mockCreated);
      issueChangeDtoMockedStatic.when(() -> IssueChangeDto.of(anyString(), any(FieldDiffs.class), anyString()))
        .thenReturn(mockAnticipatedTransition);
      underTest.insertChanges(issueChangeMapper, issue, uuidFactory);
    }
    verify(issueChangeMapper, times(2)).insert(any(IssueChangeDto.class));
    verify(issueChangeMapper).insert(mockCreated);
    verify(issueChangeMapper).insert(mockAnticipatedTransition);
  }
}
