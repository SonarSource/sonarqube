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
package org.sonar.server.computation.issue;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.source.SourceService;

/**
 * Detect the SCM author and SQ assignee.
 * <p/>
 * It relies on:
 * <ul>
 *   <li>SCM information sent in the report for modified files</li>
 *   <li>sources lines stored in database for non-modified files</li>
 * </ul>
 */
public class IssueAssigner extends IssueVisitor {

  private final DbClient dbClient;
  private final SourceService sourceService;
  private final BatchReportReader reportReader;
  private final DefaultAssignee defaultAssigne;
  private final ScmAccountToUser scmAccountToUser;

  private long lastCommitDate = 0L;
  private String lastCommitAuthor = null;
  private BatchReport.Changesets scmChangesets = null;

  public IssueAssigner(DbClient dbClient, SourceService sourceService, BatchReportReader reportReader,
    ScmAccountToUser scmAccountToUser, DefaultAssignee defaultAssigne) {
    this.dbClient = dbClient;
    this.sourceService = sourceService;
    this.reportReader = reportReader;
    this.scmAccountToUser = scmAccountToUser;
    this.defaultAssigne = defaultAssigne;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.isNew()) {
      // optimization - do not load SCM data of this component if there are no new issues
      loadScmChangesetsIfNeeded(component);

      String scmAuthor = guessScmAuthor(issue.line());
      issue.setAuthorLogin(scmAuthor);
      if (scmAuthor != null) {
        String assigneeLogin = scmAccountToUser.getNullable(scmAuthor);
        if (assigneeLogin == null) {
          issue.setAssignee(defaultAssigne.getLogin());
        } else {
          issue.setAssignee(assigneeLogin);
        }
      }
    }
  }

  private void loadScmChangesetsIfNeeded(Component component) {
    if (scmChangesets == null) {
      scmChangesets = loadScmChangesetsFromReport(component);
      if (scmChangesets == null) {
        scmChangesets = loadScmChangesetsFromDb(component);
      }
      computeLastCommitDateAndAuthor();
    }
  }

  @Override
  public void afterComponent(Component component) {
    lastCommitDate = 0L;
    lastCommitAuthor = null;
    scmChangesets = null;
  }

  /**
   * Get the SCM login of the last committer on the line. When line is zero,
   * then get the last committer on the file.
   */
  @CheckForNull
  private String guessScmAuthor(@Nullable Integer line) {
    String author = null;
    if (line != null && line <= scmChangesets.getChangesetIndexByLineCount()) {
      BatchReport.Changesets.Changeset changeset = scmChangesets.getChangeset(scmChangesets.getChangesetIndexByLine(line - 1));
      author = changeset.hasAuthor() ? changeset.getAuthor() : null;
    }
    return StringUtils.defaultIfEmpty(author, lastCommitAuthor);
  }

  private BatchReport.Changesets loadScmChangesetsFromReport(Component component) {
    return reportReader.readChangesets(component.getReportAttributes().getRef());
  }

  private BatchReport.Changesets loadScmChangesetsFromDb(Component component) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<Iterable<DbFileSources.Line>> lines = sourceService.getLines(dbSession, component.getUuid(), 1, Integer.MAX_VALUE);
      Map<String, BatchReport.Changesets.Changeset> changesetByRevision = new HashMap<>();
      BatchReport.Changesets.Builder scmBuilder = BatchReport.Changesets.newBuilder()
        .setComponentRef(component.getReportAttributes().getRef());
      if (lines.isPresent()) {
        for (DbFileSources.Line sourceLine : lines.get()) {
          String scmRevision = sourceLine.getScmRevision();
          if (scmRevision == null || changesetByRevision.get(scmRevision) == null) {
            BatchReport.Changesets.Changeset.Builder changeSetBuilder = BatchReport.Changesets.Changeset.newBuilder();
            if (sourceLine.hasScmAuthor()) {
              changeSetBuilder.setAuthor(sourceLine.getScmAuthor());
            }
            if (sourceLine.hasScmDate()) {
              changeSetBuilder.setDate(sourceLine.getScmDate());
            }
            if (scmRevision != null) {
              changeSetBuilder.setRevision(scmRevision);
            }

            BatchReport.Changesets.Changeset changeset = changeSetBuilder.build();
            scmBuilder.addChangeset(changeset);
            scmBuilder.addChangesetIndexByLine(scmBuilder.getChangesetCount() - 1);
            if (scmRevision != null) {
              changesetByRevision.put(scmRevision, changeset);
            }
          } else {
            scmBuilder.addChangesetIndexByLine(scmBuilder.getChangesetList().indexOf(changesetByRevision.get(scmRevision)));
          }
        }
      }
      return scmBuilder.build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void computeLastCommitDateAndAuthor() {
    for (BatchReport.Changesets.Changeset changeset : scmChangesets.getChangesetList()) {
      if (changeset.hasAuthor() && changeset.hasDate() && changeset.getDate() > lastCommitDate) {
        lastCommitDate = changeset.getDate();
        lastCommitAuthor = changeset.getAuthor();
      }
    }
  }
}
