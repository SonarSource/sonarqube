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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import org.sonar.api.utils.DateUtils;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;

/**
 * Calculates the creation date of an issue. A new issue is always backdated to the date of the latest SCM changeset of
 * the lines it points to, so that an issue raised on code which has not been changed does not show up in New Code.
 * Issues for which no SCM information is available keep the analysis date.
 */
public class IssueCreationDateCalculator extends IssueVisitor {

  private final ScmInfoRepository scmInfoRepository;
  private final IssueFieldsSetter issueUpdater;
  private final IssueChangeContext changeContext;

  public IssueCreationDateCalculator(AnalysisMetadataHolder analysisMetadataHolder, ScmInfoRepository scmInfoRepository,
    IssueFieldsSetter issueUpdater) {
    this.scmInfoRepository = scmInfoRepository;
    this.issueUpdater = issueUpdater;
    this.changeContext = issueChangeContextByScanBuilder(new Date(analysisMetadataHolder.getAnalysisDate())).build();
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (!issue.isNew()) {
      return;
    }

    getDateOfLatestChange(component, issue).ifPresent(changeDate -> updateDate(issue, changeDate));
  }

  private Optional<Date> getDateOfLatestChange(Component component, DefaultIssue issue) {
    return getScmInfo(component)
      .flatMap(scmInfo -> getLatestChangeset(component, scmInfo, issue))
      .map(IssueCreationDateCalculator::getChangeDate);
  }

  private Optional<ScmInfo> getScmInfo(Component component) {
    return scmInfoRepository.getScmInfo(component);
  }

  private static Optional<Changeset> getLatestChangeset(Component component, ScmInfo scmInfo, DefaultIssue issue) {
    Optional<Changeset> mostRecentChangeset = IssueLocations.allLinesFor(issue, component.getUuid())
      .filter(scmInfo::hasChangesetForLine)
      .mapToObj(scmInfo::getChangesetForLine)
      .max(Comparator.comparingLong(Changeset::getDate));
    if (mostRecentChangeset.isPresent()) {
      return mostRecentChangeset;
    }
    return Optional.of(scmInfo.getLatestChangeset());
  }

  private static Date getChangeDate(Changeset changesetForLine) {
    return DateUtils.longToDate(changesetForLine.getDate());
  }

  private void updateDate(DefaultIssue issue, Date scmDate) {
    issueUpdater.setCreationDate(issue, scmDate, changeContext);
  }
}
