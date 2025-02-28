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
package org.sonar.ce.task.projectanalysis.pushevent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.Rule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.ce.task.projectanalysis.locations.flow.FlowGenerator;
import org.sonar.ce.task.projectanalysis.locations.flow.Location;
import org.sonar.ce.task.projectanalysis.locations.flow.TextRange;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.security.SecurityStandards;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;

@ComputeEngineSide
public class PushEventFactory {
  private static final Gson GSON = new GsonBuilder().create();

  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TaintChecker taintChecker;
  private final FlowGenerator flowGenerator;
  private final RuleRepository ruleRepository;

  public PushEventFactory(TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder, TaintChecker taintChecker,
    FlowGenerator flowGenerator, RuleRepository ruleRepository) {
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.taintChecker = taintChecker;
    this.flowGenerator = flowGenerator;
    this.ruleRepository = ruleRepository;
  }

  public Optional<PushEventDto> raiseEventOnIssue(String projectUuid, DefaultIssue currentIssue) {
    var currentIssueComponentUuid = currentIssue.componentUuid();
    if (currentIssueComponentUuid == null) {
      return Optional.empty();
    }

    var component = treeRootHolder.getComponentByUuid(currentIssueComponentUuid);

    if (isTaintVulnerability(currentIssue)) {
      return raiseTaintVulnerabilityEvent(projectUuid, component, currentIssue);
    }
    if (isSecurityHotspot(currentIssue)) {
      return raiseSecurityHotspotEvent(projectUuid, component, currentIssue);
    }
    return Optional.empty();
  }

  private boolean isTaintVulnerability(DefaultIssue issue) {
    return taintChecker.isTaintVulnerability(issue);
  }

  private static boolean isSecurityHotspot(DefaultIssue issue) {
    return RuleType.SECURITY_HOTSPOT.equals(issue.type());
  }

  private Optional<PushEventDto> raiseTaintVulnerabilityEvent(String projectUuid, Component component, DefaultIssue issue) {
    if (shouldCreateRaisedEvent(issue)) {
      return Optional.of(raiseTaintVulnerabilityRaisedEvent(projectUuid, component, issue));
    }
    if (issue.isBeingClosed()) {
      return Optional.of(raiseTaintVulnerabilityClosedEvent(projectUuid, issue));
    }
    return Optional.empty();
  }

  private Optional<PushEventDto> raiseSecurityHotspotEvent(String projectUuid, Component component, DefaultIssue issue) {
    if (shouldCreateRaisedEvent(issue)) {
      return Optional.of(raiseSecurityHotspotRaisedEvent(projectUuid, component, issue));
    }
    if (issue.isBeingClosed()) {
      return Optional.of(raiseSecurityHotspotClosedEvent(projectUuid, component, issue));
    }
    return Optional.empty();
  }

  private static boolean shouldCreateRaisedEvent(DefaultIssue issue) {
    return issue.isNew() || issue.isCopied() || isReopened(issue);
  }

  private static boolean isReopened(DefaultIssue currentIssue) {
    var currentChange = currentIssue.currentChange();
    if (currentChange == null) {
      return false;
    }
    var status = currentChange.get("status");
    return status != null && Set.of("CLOSED|OPEN", "CLOSED|TO_REVIEW").contains(status.toString());
  }

  private PushEventDto raiseTaintVulnerabilityRaisedEvent(String projectUuid, Component component, DefaultIssue issue) {
    TaintVulnerabilityRaised event = prepareEvent(component, issue);
    return createPushEventDto(projectUuid, issue, event);
  }

  private TaintVulnerabilityRaised prepareEvent(Component component, DefaultIssue issue) {
    TaintVulnerabilityRaised event = new TaintVulnerabilityRaised();
    event.setProjectKey(issue.projectKey());
    event.setCreationDate(issue.creationDate().getTime());
    event.setKey(issue.key());
    event.setSeverity(issue.severity());
    event.setRuleKey(issue.getRuleKey().toString());
    event.setType(issue.type().name());
    event.setBranch(analysisMetadataHolder.getBranch().getName());
    event.setMainLocation(prepareMainLocation(component, issue));
    event.setFlows(flowGenerator.convertFlows(component.getName(), requireNonNull(issue.getLocations())));
    issue.getRuleDescriptionContextKey().ifPresent(event::setRuleDescriptionContextKey);

    Rule rule = ruleRepository.getByKey(issue.getRuleKey());
    CleanCodeAttribute cleanCodeAttribute = requireNonNull(rule.cleanCodeAttribute());
    event.setCleanCodeAttribute(cleanCodeAttribute.name());
    event.setCleanCodeAttributeCategory(cleanCodeAttribute.getAttributeCategory().name());
    event.setImpacts(computeEffectiveImpacts(rule.getDefaultImpacts(), issue.impacts()));
    return event;
  }

  private static List<TaintVulnerabilityRaised.Impact> computeEffectiveImpacts(Map<SoftwareQuality, Severity> defaultImpacts, Map<SoftwareQuality, Severity> impacts) {
    Map<SoftwareQuality, Severity> impactMap = new EnumMap<>(defaultImpacts);
    impacts.forEach((softwareQuality, severity) -> impactMap.computeIfPresent(softwareQuality, (existingSoftwareQuality, existingSeverity) -> severity));
    return impactMap.entrySet().stream()
      .map(e -> {
        TaintVulnerabilityRaised.Impact impact = new TaintVulnerabilityRaised.Impact();
        impact.setSoftwareQuality(e.getKey().name());
        impact.setSeverity(e.getValue().name());
        return impact;
      }).toList();
  }

  private static Location prepareMainLocation(Component component, DefaultIssue issue) {
    DbIssues.Locations issueLocations = requireNonNull(issue.getLocations());
    TextRange mainLocationTextRange = getTextRange(issueLocations.getTextRange(), issueLocations.getChecksum());

    Location mainLocation = new Location();
    Optional.ofNullable(issue.getMessage()).ifPresent(mainLocation::setMessage);
    mainLocation.setFilePath(component.getName());
    mainLocation.setTextRange(mainLocationTextRange);
    return mainLocation;
  }

  private static PushEventDto createPushEventDto(String projectUuid, DefaultIssue issue, IssueEvent event) {
    return new PushEventDto()
      .setName(event.getEventName())
      .setProjectUuid(projectUuid)
      .setLanguage(issue.language())
      .setPayload(serializeEvent(event));
  }

  private static PushEventDto raiseTaintVulnerabilityClosedEvent(String projectUuid, DefaultIssue issue) {
    TaintVulnerabilityClosed event = new TaintVulnerabilityClosed(issue.key(), issue.projectKey());
    return createPushEventDto(projectUuid, issue, event);
  }

  private PushEventDto raiseSecurityHotspotRaisedEvent(String projectUuid, Component component, DefaultIssue issue) {
    SecurityHotspotRaised event = new SecurityHotspotRaised();
    event.setKey(issue.key());
    event.setProjectKey(issue.projectKey());
    event.setStatus(issue.getStatus());
    event.setCreationDate(issue.creationDate().getTime());
    event.setMainLocation(prepareMainLocation(component, issue));
    event.setRuleKey(issue.getRuleKey().toString());
    event.setVulnerabilityProbability(getVulnerabilityProbability(issue));
    event.setBranch(analysisMetadataHolder.getBranch().getName());
    event.setAssignee(issue.assigneeLogin());

    return createPushEventDto(projectUuid, issue, event);
  }

  private String getVulnerabilityProbability(DefaultIssue issue) {
    Rule rule = ruleRepository.getByKey(issue.getRuleKey());
    SecurityStandards.SQCategory sqCategory = fromSecurityStandards(rule.getSecurityStandards()).getSqCategory();
    return sqCategory.getVulnerability().name();
  }

  private static PushEventDto raiseSecurityHotspotClosedEvent(String projectUuid, Component component, DefaultIssue issue) {
    SecurityHotspotClosed event = new SecurityHotspotClosed();
    event.setKey(issue.key());
    event.setProjectKey(issue.projectKey());
    event.setStatus(issue.getStatus());
    event.setResolution(issue.resolution());
    event.setFilePath(component.getName());

    return createPushEventDto(projectUuid, issue, event);
  }

  private static byte[] serializeEvent(IssueEvent event) {
    return GSON.toJson(event).getBytes(UTF_8);
  }

  @NotNull
  private static TextRange getTextRange(DbCommons.TextRange source, String checksum) {
    TextRange textRange = new TextRange();
    textRange.setStartLine(source.getStartLine());
    textRange.setStartLineOffset(source.getStartOffset());
    textRange.setEndLine(source.getEndLine());
    textRange.setEndLineOffset(source.getEndOffset());
    textRange.setHash(checksum);
    return textRange;
  }

}
