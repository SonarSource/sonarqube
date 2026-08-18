/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.source;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.issue.SecretIssueRedactionRules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceServiceTest {
  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final FileSourceDao fileSourceDao = mock(FileSourceDao.class);
  private final IssueDao issueDao = mock(IssueDao.class);
  private final SourceService underTest = new SourceService(dbClient, mock(HtmlSourceDecorator.class));

  @Test
  void getLines_whenSourceIsRequested_shouldLoadComponentSecretIssues() {
    when(dbClient.fileSourceDao()).thenReturn(fileSourceDao);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(fileSourceDao.selectByFileUuid(dbSession, "file-uuid")).thenReturn(fileSource("project-uuid", "secret-value"));
    when(issueDao.selectSourceRedactionIssues(dbSession, "file-uuid", SecretIssueRedactionRules.sourceRedactionRuleKeys())).thenReturn(List.of());

    Iterable<DbFileSources.Line> lines = underTest.getLines(dbSession, "file-uuid", 1, 1).orElseThrow();

    assertThat(lines).extracting(DbFileSources.Line::getSource).containsExactly("secret-value");
    verify(issueDao).selectSourceRedactionIssues(dbSession, "file-uuid", SecretIssueRedactionRules.sourceRedactionRuleKeys());
  }

  @Test
  void getScmLines_whenScmSourceIsRequested_shouldNotLoadSecretIssues() {
    when(dbClient.fileSourceDao()).thenReturn(fileSourceDao);
    when(fileSourceDao.selectByFileUuid(dbSession, "file-uuid")).thenReturn(fileSource("project-uuid", "secret-value"));

    Iterable<DbFileSources.Line> lines = underTest.getScmLines(dbSession, "file-uuid", 1, 1).orElseThrow();

    assertThat(lines).extracting(DbFileSources.Line::getSource).containsExactly("secret-value");
    verify(dbClient, never()).issueDao();
  }

  private static FileSourceDto fileSource(String projectUuid, String source) {
    return new FileSourceDto().setProjectUuid(projectUuid).setSourceData(DbFileSources.Data.newBuilder()
      .addLines(DbFileSources.Line.newBuilder().setLine(1).setSource(source))
      .build());
  }
}
