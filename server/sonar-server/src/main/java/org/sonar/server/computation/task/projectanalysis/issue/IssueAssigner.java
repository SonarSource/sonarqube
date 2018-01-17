/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
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
    if (issue.authorLogin() == null) {
      loadScmChangesets(component);
      String scmAuthor = guessScmAuthor(issue);

      if (!Strings.isNullOrEmpty(scmAuthor)) {
        if (scmAuthor.length() <= IssueDto.AUTHOR_MAX_SIZE) {
          issueUpdater.setNewAuthor(issue, scmAuthor, changeContext);
        } else {
          LOGGER.debug("SCM account '{}' is too long to be stored as issue author", scmAuthor);
        }
      }

      if (issue.assignee() == null) {
        String author = Strings.isNullOrEmpty(scmAuthor) ? null : scmAccountToUser.getNullable(scmAuthor);
        String assigneeLogin = StringUtils.defaultIfEmpty(author, defaultAssignee.loadDefaultAssigneeLogin());

        issueUpdater.setNewAssignee(issue, assigneeLogin, changeContext);
      }
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
   * Get the SCM login of the last committer on the line. When line is zero,
   * then get the last committer on the file.
   */
  @CheckForNull
  private String guessScmAuthor(DefaultIssue issue) {
    Integer line = issue.line();
    String author = null;
    if (line != null && scmChangesets != null) {
      if (scmChangesets.hasChangesetForLine(line)) {
        author = scmChangesets.getChangesetForLine(line).getAuthor();
      } else {
        LOGGER.warn("No SCM info has been found for issue {}", issue);
      }
    }
    return defaultIfEmpty(author, lastCommitAuthor);
  }
}
