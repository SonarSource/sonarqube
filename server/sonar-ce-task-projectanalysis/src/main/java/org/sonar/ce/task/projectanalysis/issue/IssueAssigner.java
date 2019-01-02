/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.sonar.core.issue.IssueChangeContext.createScan;

/**
 * Detect the SCM author and SQ assignee.
 * <p/>
 * It relies on SCM information which comes from both the report and database.
 */
public class IssueAssigner extends IssueVisitor {

  private static final Logger LOGGER = Loggers.get(IssueAssigner.class);

  private final ScmInfoRepository scmInfoRepository;
  private final DefaultAssignee defaultAssignee;
  private final IssueFieldsSetter issueUpdater;
  private final ScmAccountToUser scmAccountToUser;
  private final IssueChangeContext changeContext;

  private String lastCommitAuthor = null;
  private ScmInfo scmChangesets = null;

  public IssueAssigner(AnalysisMetadataHolder analysisMetadataHolder, ScmInfoRepository scmInfoRepository, ScmAccountToUser scmAccountToUser, DefaultAssignee defaultAssignee,
    IssueFieldsSetter issueUpdater) {
    this.scmInfoRepository = scmInfoRepository;
    this.scmAccountToUser = scmAccountToUser;
    this.defaultAssignee = defaultAssignee;
    this.issueUpdater = issueUpdater;
    this.changeContext = createScan(new Date(analysisMetadataHolder.getAnalysisDate()));
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.authorLogin() != null) {
      return;
    }
    loadScmChangesets(component);
    Optional<String> scmAuthor = guessScmAuthor(issue, component);

    if (scmAuthor.isPresent()) {
      if (scmAuthor.get().length() <= IssueDto.AUTHOR_MAX_SIZE) {
        issueUpdater.setNewAuthor(issue, scmAuthor.get(), changeContext);
      } else {
        LOGGER.debug("SCM account '{}' is too long to be stored as issue author", scmAuthor.get());
      }
    }

    if (issue.assignee() == null) {
      String assigneeUuid = scmAuthor.map(scmAccountToUser::getNullable).orElse(null);
      assigneeUuid = defaultIfEmpty(assigneeUuid, defaultAssignee.loadDefaultAssigneeUuid());
      issueUpdater.setNewAssignee(issue, assigneeUuid, changeContext);
    }
  }

  private void loadScmChangesets(Component component) {
    if (scmChangesets == null) {
      Optional<ScmInfo> scmInfoOptional = scmInfoRepository.getScmInfo(component);
      if (scmInfoOptional.isPresent()) {
        scmChangesets = scmInfoOptional.get();
        lastCommitAuthor = scmChangesets.getLatestChangeset().getAuthor();
      }
    }
  }

  @Override
  public void afterComponent(Component component) {
    lastCommitAuthor = null;
    scmChangesets = null;
  }

  /**
   * Author of the latest change on the lines involved by the issue.
   * If no authors are set on the lines, then the author of the latest change on the file
   * is returned.
   */
  private Optional<String> guessScmAuthor(DefaultIssue issue, Component component) {
    String author = null;
    if (scmChangesets != null) {
      author = IssueLocations.allLinesFor(issue, component.getUuid())
        .filter(scmChangesets::hasChangesetForLine)
        .mapToObj(scmChangesets::getChangesetForLine)
        .filter(c -> StringUtils.isNotEmpty(c.getAuthor()))
        .max(Comparator.comparingLong(Changeset::getDate))
        .map(Changeset::getAuthor)
        .orElse(null);
    }
    return Optional.ofNullable(defaultIfEmpty(author, lastCommitAuthor));
  }
}
