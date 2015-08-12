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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;

/**
 * Detect the SCM author and SQ assignee.
 * <p/>
 * It relies on:
 * <ul>
 *   <li>the SCM information sent in the report for modified files</li>
 *   <li>the Elasticsearch index of source lines for non-modified files</li>
 * </ul>
 */
public class IssueAssigner extends IssueVisitor {

  private final SourceLineIndex sourceLineIndex;
  private final BatchReportReader reportReader;
  private final DefaultAssignee defaultAssigne;
  private final ScmAccountToUser scmAccountToUser;

  private long lastCommitDate = 0L;
  private String lastCommitAuthor = null;
  private BatchReport.Changesets scmChangesets = null;

  public IssueAssigner(SourceLineIndex sourceLineIndex, BatchReportReader reportReader,
    ScmAccountToUser scmAccountToUser, DefaultAssignee defaultAssigne) {
    this.sourceLineIndex = sourceLineIndex;
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
        scmChangesets = loadScmChangesetsFromIndex(component);
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

  private BatchReport.Changesets loadScmChangesetsFromIndex(Component component) {
    List<SourceLineDoc> lines = sourceLineIndex.getLines(component.getUuid());
    Map<String, BatchReport.Changesets.Changeset> changesetByRevision = new HashMap<>();
    BatchReport.Changesets.Builder scmBuilder = BatchReport.Changesets.newBuilder()
      .setComponentRef(component.getReportAttributes().getRef());
    for (SourceLineDoc sourceLine : lines) {
      String scmRevision = sourceLine.scmRevision();
      if (scmRevision == null || changesetByRevision.get(scmRevision) == null) {
        BatchReport.Changesets.Changeset.Builder changeSetBuilder = BatchReport.Changesets.Changeset.newBuilder();
        String scmAuthor = sourceLine.scmAuthor();
        if (scmAuthor != null) {
          changeSetBuilder.setAuthor(scmAuthor);
        }
        Date scmDate = sourceLine.scmDate();
        if (scmDate != null) {
          changeSetBuilder.setDate(scmDate.getTime());
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
    return scmBuilder.build();
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
