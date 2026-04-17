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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.ce.task.projectanalysis.util.TextRangeUtils;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.user.UserIdDto;
import org.sonar.scanner.protocol.output.ScannerReport.IssueResolution;
import org.sonar.scanner.protocol.output.ScannerReport.IssueResolutionStatus;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition;

/**
 * Transitions issues holding issue resolution data from the scanner report.
 */
public class IssueResolutionVisitor extends IssueVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(IssueResolutionVisitor.class);

  private static final Map<IssueResolutionStatus, CodeQualityIssueWorkflowTransition> ISSUE_TRANSITIONS = Map.of(
    IssueResolutionStatus.DEFAULT, CodeQualityIssueWorkflowTransition.ACCEPT,
    IssueResolutionStatus.FALSE_POSITIVE, CodeQualityIssueWorkflowTransition.FALSE_POSITIVE);

  private static final Map<IssueResolutionStatus, SecurityHotspotWorkflowTransition> HOTSPOT_TRANSITIONS = Map.of(
    IssueResolutionStatus.DEFAULT, SecurityHotspotWorkflowTransition.RESOLVE_AS_ACKNOWLEDGED,
    IssueResolutionStatus.FALSE_POSITIVE, SecurityHotspotWorkflowTransition.RESOLVE_AS_SAFE);

  private static final Map<IssueResolutionStatus, IssueStatus> ISSUE_TARGET_STATUS = Map.of(
    IssueResolutionStatus.DEFAULT, IssueStatus.ACCEPTED,
    IssueResolutionStatus.FALSE_POSITIVE, IssueStatus.FALSE_POSITIVE);

  private static final Map<IssueResolutionStatus, String> HOTSPOT_TARGET_RESOLUTIONS = Map.of(
    IssueResolutionStatus.DEFAULT, Issue.RESOLUTION_ACKNOWLEDGED,
    IssueResolutionStatus.FALSE_POSITIVE, Issue.RESOLUTION_SAFE);

  private final IssueLifecycle issueLifecycle;
  private final ScmInfoRepository scmInfoRepository;
  private final ScmAccountToUser scmAccountToUser;
  private final ScannerReportReader reportReader;
  private final ConfigurationRepository configurationRepository;

  private ScmInfo scmInfo = null;
  private boolean issueResolutionEnabled = false;
  private List<IssueResolution> componentIssueResolution;

  public IssueResolutionVisitor(IssueLifecycle issueLifecycle,
    ScmInfoRepository scmInfoRepository, ScmAccountToUser scmAccountToUser, ScannerReportReader reportReader,
    ConfigurationRepository configurationRepository) {
    this.issueLifecycle = issueLifecycle;
    this.scmInfoRepository = scmInfoRepository;
    this.scmAccountToUser = scmAccountToUser;
    this.reportReader = reportReader;
    this.configurationRepository = configurationRepository;
  }

  @Override
  public void beforeComponent(Component component) {
    scmInfo = scmInfoRepository.getScmInfo(component).orElse(null);
    issueResolutionEnabled = isIssueResolutionEnabled();
    componentIssueResolution = issueResolutionEnabled
      ? Optional.ofNullable(component.getReportAttributes().getRef()).map(this::getIssueResolutionForComponent).orElse(null)
      : null;
  }

  @Override
  public void afterComponent(Component component) {
    scmInfo = null;
    issueResolutionEnabled = false;
    componentIssueResolution = null;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (!issueResolutionEnabled) {
      if (issue.internalTags().contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG)) {
        reopenAndRemoveTag(issue);
      }
      return;
    }

    IssueResolution issueResolution = lookForIssueResolution(issue);
    boolean hasTag = issue.internalTags().contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);

    if (issueResolution != null) {
      applyIssueResolution(issue, issueResolution);
      if (!hasTag) {
        addTag(issue);
      }
    } else if (hasTag) {
      reopenAndRemoveTag(issue);
    }
  }

  private void applyIssueResolution(DefaultIssue issue, IssueResolution issueResolutionData) {
    IssueResolutionStatus scannerStatus = issueResolutionData.getStatus();
    boolean alreadyInTargetState;
    String targetTransitionKey;

    if (issue.type() == RuleType.SECURITY_HOTSPOT) {
      String targetResolution = HOTSPOT_TARGET_RESOLUTIONS.get(scannerStatus);
      alreadyInTargetState = targetResolution.equals(issue.resolution());
      targetTransitionKey = HOTSPOT_TRANSITIONS.get(scannerStatus).getKey();
    } else {
      IssueStatus targetStatus = ISSUE_TARGET_STATUS.get(scannerStatus);
      alreadyInTargetState = targetStatus == issue.issueStatus();
      targetTransitionKey = ISSUE_TRANSITIONS.get(scannerStatus).getKey();
    }

    String reopenTransitionKey = getReopenTransitionKey(issue);

    if (alreadyInTargetState) {
      return;
    }

    String userUuid = resolveIssueResolutionAuthorUuid(issueResolutionData).orElse(null);
    try {
      if (reopenTransitionKey != null) {
        issueLifecycle.doManualTransition(issue, reopenTransitionKey, userUuid);
      }
      issueLifecycle.doManualTransition(issue, targetTransitionKey, userUuid);
      String comment = issueResolutionData.getComment();
      if (StringUtils.isNotBlank(comment)) {
        issueLifecycle.addComment(issue, "issue-resolution: " + comment, userUuid);
      }
    } catch (Exception e) {
      LOGGER.warn("Cannot apply issue resolution data on issue at line {} of {}", issue.getLine(), issue.componentKey(), e);
    }
  }

  private void reopenAndRemoveTag(DefaultIssue issue) {
    String reopenTransitionKey = getReopenTransitionKey(issue);
    if (reopenTransitionKey == null) {
      removeTag(issue);
      return;
    }
    try {
      issueLifecycle.doManualTransition(issue, reopenTransitionKey, null);
      removeTag(issue);
    } catch (Exception e) {
      LOGGER.warn("Cannot reopen issue-resolution issue at line {} of {}", issue.getLine(), issue.componentKey(), e);
    }
  }

  @CheckForNull
  private static String getReopenTransitionKey(DefaultIssue issue) {
    if (issue.type() == RuleType.SECURITY_HOTSPOT) {
      return Issue.STATUS_REVIEWED.equals(issue.status())
        ? SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW.getKey()
        : null;
    }
    IssueStatus status = issue.issueStatus();
    return (status == IssueStatus.ACCEPTED || status == IssueStatus.FALSE_POSITIVE)
      ? CodeQualityIssueWorkflowTransition.REOPEN.getKey()
      : null;
  }

  private static void addTag(DefaultIssue issue) {
    Set<String> tags = new LinkedHashSet<>(issue.internalTags());
    tags.add(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
    issue.setInternalTags(tags);
    issue.setChanged(true);
  }

  private static void removeTag(DefaultIssue issue) {
    Set<String> tags = new LinkedHashSet<>(issue.internalTags());
    if (tags.remove(IssueFieldsSetter.ISSUE_RESOLUTION_TAG)) {
      issue.setInternalTags(tags);
      issue.setChanged(true);
    }
  }

  private List<IssueResolution> getIssueResolutionForComponent(int componentRef) {
    List<IssueResolution> dataList = new ArrayList<>();
    try (CloseableIterator<IssueResolution> iter = reportReader.readIssueResolution(componentRef)) {
      while (iter.hasNext()) {
        dataList.add(iter.next());
      }
    }
    return dataList;
  }

  @CheckForNull
  private IssueResolution lookForIssueResolution(DefaultIssue issue) {
    if (componentIssueResolution == null) {
      return null;
    }
    String ruleKeyStr = issue.ruleKey().toString();
    for (IssueResolution issueResolution : componentIssueResolution) {
      if (!issueResolution.getRuleKeysList().contains(ruleKeyStr)) {
        continue;
      }
      // Check if the issue's line falls within the text range
      Integer line = issue.getLine();
      if (line != null && issueResolution.hasTextRange()
        && TextRangeUtils.containsLine(issueResolution.getTextRange(), line)) {
        return issueResolution;
      }
    }
    return null;
  }

  private boolean isIssueResolutionEnabled() {
    Configuration config = configurationRepository.getConfiguration();
    boolean globalGateEnabled = config.getBoolean(CorePropertyDefinitions.ISSUE_RESOLUTION_GLOBAL_ENABLED).orElse(false);
    boolean projectEnabled = config.getBoolean(CorePropertyDefinitions.ISSUE_RESOLUTION_ENABLED).orElse(false);
    return globalGateEnabled && projectEnabled;
  }

  private Optional<String> resolveIssueResolutionAuthorUuid(IssueResolution issueResolution) {
    if (!issueResolution.hasTextRange()) {
      return Optional.empty();
    }
    int line = issueResolution.getTextRange().getStartLine();
    return Optional.ofNullable(scmInfo)
      .filter(info -> info.hasChangesetForLine(line))
      .map(info -> info.getChangesetForLine(line))
      .map(Changeset::getAuthor)
      .filter(StringUtils::isNotEmpty)
      .map(scmAccountToUser::getNullable)
      .map(UserIdDto::getUuid);
  }
}
