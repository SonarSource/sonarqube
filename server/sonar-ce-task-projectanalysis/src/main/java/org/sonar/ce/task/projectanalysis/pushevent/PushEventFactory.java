/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.locations.flow.FlowGenerator;
import org.sonar.ce.task.projectanalysis.locations.flow.Location;
import org.sonar.ce.task.projectanalysis.locations.flow.TextRange;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.issue.TaintChecker;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

@ComputeEngineSide
public class PushEventFactory {
  private static final Gson GSON = new GsonBuilder().create();

  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TaintChecker taintChecker;
  private final FlowGenerator flowGenerator;

  public PushEventFactory(TreeRootHolder treeRootHolder,
    AnalysisMetadataHolder analysisMetadataHolder, TaintChecker taintChecker, FlowGenerator flowGenerator) {
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.taintChecker = taintChecker;
    this.flowGenerator = flowGenerator;
  }

  public Optional<PushEventDto> raiseEventOnIssue(String projectUuid, DefaultIssue currentIssue) {
    var currentIssueComponentUuid = currentIssue.componentUuid();
    if (!taintChecker.isTaintVulnerability(currentIssue) || currentIssueComponentUuid == null) {
      return Optional.empty();
    }

    var component = treeRootHolder.getComponentByUuid(Objects.requireNonNull(currentIssue.componentUuid()));
    if (currentIssue.isNew() || currentIssue.isCopied() || isReopened(currentIssue)) {
      return Optional.of(raiseTaintVulnerabilityRaisedEvent(projectUuid, component, currentIssue));
    }
    if (currentIssue.isBeingClosed()) {
      return Optional.of(raiseTaintVulnerabilityClosedEvent(projectUuid, currentIssue));
    }
    return Optional.empty();
  }

  private static boolean isReopened(DefaultIssue currentIssue) {
    var currentChange = currentIssue.currentChange();
    if (currentChange == null) {
      return false;
    }
    var status = currentChange.get("status");
    return status != null && status.toString().equals("CLOSED|OPEN");
  }

  private PushEventDto raiseTaintVulnerabilityRaisedEvent(String projectUuid, Component component, DefaultIssue issue) {
    TaintVulnerabilityRaised event = prepareEvent(component, issue);
    return new PushEventDto()
      .setName("TaintVulnerabilityRaised")
      .setProjectUuid(projectUuid)
      .setLanguage(issue.language())
      .setPayload(serializeEvent(event));
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
    return event;
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

  private static PushEventDto raiseTaintVulnerabilityClosedEvent(String projectUuid, DefaultIssue issue) {
    TaintVulnerabilityClosed event = new TaintVulnerabilityClosed(issue.key(), issue.projectKey());
    return new PushEventDto()
      .setName("TaintVulnerabilityClosed")
      .setProjectUuid(projectUuid)
      .setLanguage(issue.language())
      .setPayload(serializeEvent(event));
  }

  private static byte[] serializeEvent(Object event) {
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
