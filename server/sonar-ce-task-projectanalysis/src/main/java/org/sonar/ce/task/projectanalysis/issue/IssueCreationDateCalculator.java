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
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.filemove.AddedFileRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UNCHANGED;
import static org.sonar.core.issue.IssueChangeContext.createScan;

/**
 * Calculates the creation date of an issue. Takes into account, that the issue
 * might be raised by adding a rule to a quality profile.
 */
public class IssueCreationDateCalculator extends IssueVisitor {

  private final ScmInfoRepository scmInfoRepository;
  private final IssueFieldsSetter issueUpdater;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final IssueChangeContext changeContext;
  private final ActiveRulesHolder activeRulesHolder;
  private final RuleRepository ruleRepository;
  private final AddedFileRepository addedFileRepository;
  private QProfileStatusRepository qProfileStatusRepository;

  public IssueCreationDateCalculator(AnalysisMetadataHolder analysisMetadataHolder, ScmInfoRepository scmInfoRepository,
    IssueFieldsSetter issueUpdater, ActiveRulesHolder activeRulesHolder, RuleRepository ruleRepository,
    AddedFileRepository addedFileRepository, QProfileStatusRepository qProfileStatusRepository) {
    this.scmInfoRepository = scmInfoRepository;
    this.issueUpdater = issueUpdater;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.ruleRepository = ruleRepository;
    this.changeContext = createScan(new Date(analysisMetadataHolder.getAnalysisDate()));
    this.activeRulesHolder = activeRulesHolder;
    this.addedFileRepository = addedFileRepository;
    this.qProfileStatusRepository = qProfileStatusRepository;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (!issue.isNew()) {
      return;
    }

    Optional<Long> lastAnalysisOptional = lastAnalysis();
    boolean firstAnalysis = !lastAnalysisOptional.isPresent();
    if (firstAnalysis || isNewFile(component)) {
      backdateIssue(component, issue);
      return;
    }

    Rule rule = ruleRepository.findByKey(issue.getRuleKey())
      .orElseThrow(illegalStateException("The rule with key '%s' raised an issue, but no rule with that key was found", issue.getRuleKey()));
    if (rule.isExternal()) {
      backdateIssue(component, issue);
    } else {
      // Rule can't be inactive (see contract of IssueVisitor)
      ActiveRule activeRule = activeRulesHolder.get(issue.getRuleKey()).get();
      if (activeRuleIsNewOrChanged(activeRule, lastAnalysisOptional.get())
        || ruleImplementationChanged(activeRule.getRuleKey(), activeRule.getPluginKey(), lastAnalysisOptional.get())
        || qualityProfileChanged(activeRule.getQProfileKey())) {
        backdateIssue(component, issue);
      }
    }
  }

  private boolean qualityProfileChanged(String qpKey) {
    return qProfileStatusRepository.get(qpKey).filter(s -> !s.equals(UNCHANGED)).isPresent();
  }

  private boolean isNewFile(Component component) {
    return component.getType() == Component.Type.FILE && addedFileRepository.isAdded(component);
  }

  private void backdateIssue(Component component, DefaultIssue issue) {
    getDateOfLatestChange(component, issue).ifPresent(changeDate -> updateDate(issue, changeDate));
  }

  private boolean ruleImplementationChanged(RuleKey ruleKey, @Nullable String pluginKey, long lastAnalysisDate) {
    if (pluginKey == null) {
      return false;
    }

    ScannerPlugin scannerPlugin = Optional.ofNullable(analysisMetadataHolder.getScannerPluginsByKey().get(pluginKey))
      .orElseThrow(illegalStateException("The rule %s is declared to come from plugin %s, but this plugin was not used by scanner.", ruleKey, pluginKey));
    return pluginIsNew(scannerPlugin, lastAnalysisDate)
      || basePluginIsNew(scannerPlugin, lastAnalysisDate);
  }

  private boolean basePluginIsNew(ScannerPlugin scannerPlugin, long lastAnalysisDate) {
    String basePluginKey = scannerPlugin.getBasePluginKey();
    if (basePluginKey == null) {
      return false;
    }
    ScannerPlugin basePlugin = analysisMetadataHolder.getScannerPluginsByKey().get(basePluginKey);
    return lastAnalysisDate < basePlugin.getUpdatedAt();
  }

  private static boolean pluginIsNew(ScannerPlugin scannerPlugin, long lastAnalysisDate) {
    return lastAnalysisDate < scannerPlugin.getUpdatedAt();
  }

  private static boolean activeRuleIsNewOrChanged(ActiveRule activeRule, Long lastAnalysisDate) {
    return lastAnalysisDate < activeRule.getUpdatedAt();
  }

  private Optional<Date> getDateOfLatestChange(Component component, DefaultIssue issue) {
    return getScmInfo(component)
      .flatMap(scmInfo -> getLatestChangeset(component, scmInfo, issue))
      .map(IssueCreationDateCalculator::getChangeDate);
  }

  private Optional<Long> lastAnalysis() {
    return Optional.ofNullable(analysisMetadataHolder.getBaseAnalysis()).map(Analysis::getCreatedAt);
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

  private static Supplier<? extends IllegalStateException> illegalStateException(String str, Object... args) {
    return () -> new IllegalStateException(String.format(str, args));
  }
}
