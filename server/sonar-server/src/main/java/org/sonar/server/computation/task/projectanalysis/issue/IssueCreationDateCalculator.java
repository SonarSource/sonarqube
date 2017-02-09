/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.computation.task.projectanalysis.analysis.Analysis;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.sonar.core.issue.IssueChangeContext.createScan;

/**
 * Calculates the creation date of an issue. Takes into account, that the issue
 * might be raised by adding a rule to a quality profile.
 */
public class IssueCreationDateCalculator extends IssueVisitor {

  private static final Logger LOGGER = Loggers.get(IssueCreationDateCalculator.class);

  private final ScmInfoRepository scmInfoRepository;
  private final IssueFieldsSetter issueUpdater;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final IssueChangeContext changeContext;
  private final ActiveRulesHolder activeRulesHolder;

  public IssueCreationDateCalculator(AnalysisMetadataHolder analysisMetadataHolder, ScmInfoRepository scmInfoRepository,
    IssueFieldsSetter issueUpdater, ActiveRulesHolder activeRulesHolder) {
    this.scmInfoRepository = scmInfoRepository;
    this.issueUpdater = issueUpdater;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.changeContext = createScan(new Date(analysisMetadataHolder.getAnalysisDate()));
    this.activeRulesHolder = activeRulesHolder;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.isNew() && ruleIsNew(issue)) {

      // get scm information of file, if available
      getChangeSet(component)
        .map(scmInfo -> Optional

          // if possible determine line change
          .ofNullable(issue.getLine()).map(scmInfo::getChangesetForLine)

          // otherwise determine file change
          .orElseGet(scmInfo::getLatestChangeset))

        // get the change's date
        .map(Changeset::getDate)
        .map(DateUtils::longToDate)

        // use the change date as the creation date of the issue
        .ifPresent(scmDate -> updateDate(issue, scmDate));
    }
  }

  private boolean ruleIsNew(DefaultIssue issue) {
    long ruleCreation = ruleCreation(issue);
    Optional<Long> lastAnalysisOptional = lastAnalysis();

    if (lastAnalysisOptional.isPresent()) {
      return lastAnalysisOptional.get() < ruleCreation;
    }

    // special case: this is the first analysis of the project: use scm dates for all issues
    return true;
  }

  private Optional<Long> lastAnalysis() {
    return Optional.ofNullable(analysisMetadataHolder.getBaseAnalysis()).map(Analysis::getCreatedAt);
  }

  private long ruleCreation(DefaultIssue issue) {
    ActiveRule activeRule = toJavaUtilOptional(activeRulesHolder.get(issue.getRuleKey()))
      .orElseThrow(() -> new IllegalStateException("The rule " + issue.getRuleKey().rule() + " raised an issue, but is not one of the active rules."));
    return activeRule.getCreatedAt();
  }

  private void updateDate(DefaultIssue issue, Date scmDate) {
    LOGGER.debug("Issue {} seems to be raised in consequence of a modification of the quality profile. Backdating the issue to {}.", issue,
      DateTimeFormatter.ISO_INSTANT.format(scmDate.toInstant()));
    issueUpdater.setCreationDate(issue, scmDate, changeContext);
  }

  private Optional<ScmInfo> getChangeSet(Component component) {
    return toJavaUtilOptional(scmInfoRepository.getScmInfo(component));
  }

  private static <T> Optional<T> toJavaUtilOptional(com.google.common.base.Optional<T> scmInfo) {
    return scmInfo.transform(Optional::of).or(Optional::empty);
  }
}
