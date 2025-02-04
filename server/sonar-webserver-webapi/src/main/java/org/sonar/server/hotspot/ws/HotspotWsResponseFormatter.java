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
package org.sonar.server.hotspot.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;
import static org.sonarqube.ws.WsUtils.nullToEmpty;

public class HotspotWsResponseFormatter {

  private final TextRangeResponseFormatter textRangeFormatter;

  public HotspotWsResponseFormatter(TextRangeResponseFormatter textRangeFormatter) {
    this.textRangeFormatter = textRangeFormatter;
  }

  Hotspots.Component formatProject(Hotspots.Component.Builder builder, ProjectDto project, @Nullable String branch, @Nullable String pullRequest) {
    builder
      .clear()
      .setKey(project.getKey())
      .setQualifier(project.getQualifier())
      .setName(project.getName())
      .setLongName(project.getName());
    ofNullable(branch).ifPresent(builder::setBranch);
    ofNullable(pullRequest).ifPresent(builder::setPullRequest);
    return builder.build();
  }

  Hotspots.Component formatComponent(Hotspots.Component.Builder builder, ComponentDto component, @Nullable String branch, @Nullable String pullRequest) {
    builder
      .clear()
      .setKey(component.getKey())
      .setQualifier(component.qualifier())
      .setName(component.name())
      .setLongName(component.longName());
    ofNullable(branch).ifPresent(builder::setBranch);
    ofNullable(pullRequest).ifPresent(builder::setPullRequest);
    ofNullable(component.path()).ifPresent(builder::setPath);
    return builder.build();
  }

  void formatHotspots(SearchResponseData searchResponseData, Hotspots.ListWsResponse.Builder responseBuilder) {
    responseBuilder.addAllHotspots(mapHotspots(searchResponseData));
  }

  void formatHotspots(SearchResponseData searchResponseData, Hotspots.SearchWsResponse.Builder responseBuilder) {
    responseBuilder.addAllHotspots(mapHotspots(searchResponseData));
  }

  private List<Hotspots.SearchWsResponse.Hotspot> mapHotspots(SearchResponseData searchResponseData) {
    List<IssueDto> hotspots = searchResponseData.getHotspots();
    if (hotspots.isEmpty()) {
      return emptyList();
    }

    Hotspots.SearchWsResponse.Hotspot.Builder builder = Hotspots.SearchWsResponse.Hotspot.newBuilder();
    List<Hotspots.SearchWsResponse.Hotspot> hotspotsList = new ArrayList<>(hotspots.size());
    for (IssueDto hotspot : hotspots) {
      SecurityStandards.SQCategory sqCategory = fromSecurityStandards(hotspot.getSecurityStandards()).getSqCategory();
      builder
        .clear()
        .setKey(hotspot.getKey())
        .setComponent(hotspot.getComponentKey())
        .setProject(hotspot.getProjectKey())
        .setSecurityCategory(sqCategory.getKey())
        .setVulnerabilityProbability(sqCategory.getVulnerability().name())
        .setRuleKey(hotspot.getRuleKey().toString());
      ofNullable(hotspot.getStatus()).ifPresent(builder::setStatus);
      ofNullable(hotspot.getResolution()).ifPresent(builder::setResolution);
      ofNullable(hotspot.getLine()).ifPresent(builder::setLine);
      builder.setMessage(nullToEmpty(hotspot.getMessage()));
      builder.addAllMessageFormattings(MessageFormattingUtils.dbMessageFormattingToWs(hotspot.parseMessageFormattings()));
      ofNullable(hotspot.getAssigneeUuid()).ifPresent(builder::setAssignee);
      builder.setAuthor(nullToEmpty(hotspot.getAuthorLogin()));
      builder.setCreationDate(formatDateTime(hotspot.getIssueCreationDate()));
      builder.setUpdateDate(formatDateTime(hotspot.getIssueUpdateDate()));
      completeHotspotLocations(hotspot, builder, searchResponseData);
      hotspotsList.add(builder.build());
    }
    return hotspotsList;
  }

  void completeHotspotLocations(IssueDto hotspot, Hotspots.SearchWsResponse.Hotspot.Builder hotspotBuilder, SearchResponseData data) {
    DbIssues.Locations locations = hotspot.parseLocations();

    if (locations == null) {
      return;
    }

    textRangeFormatter.formatTextRange(locations, hotspotBuilder::setTextRange);
    hotspotBuilder.addAllFlows(textRangeFormatter.formatFlows(locations, hotspotBuilder.getComponent(), data.getComponentsByUuid()));
  }

  Hotspots.Component formatComponent(Hotspots.Component.Builder builder, ComponentDto component, @Nullable BranchDto branchDto) {
    if (branchDto == null || branchDto.isMain()) {
      return formatComponent(builder, component, null, null);
    }
    return formatComponent(builder, component, branchDto.getBranchKey(), branchDto.getPullRequestKey());
  }

  void formatTextRange(IssueDto dto, Consumer<Common.TextRange> rangeConsumer) {
    textRangeFormatter.formatTextRange(dto, rangeConsumer);
  }

  List<Common.Flow> formatFlows(DbIssues.Locations locations, String issueComponent, Map<String, ComponentDto> componentsByUuid) {
    return textRangeFormatter.formatFlows(locations, issueComponent, componentsByUuid);
  }

  static final class SearchResponseData {
    private final Paging paging;
    private final List<IssueDto> hotspots;
    private final Map<String, ComponentDto> componentsByUuid = new HashMap<>();
    private final Map<String, BranchDto> branchesByBranchUuid = new HashMap<>();

    SearchResponseData(Paging paging, List<IssueDto> hotspots) {
      this.paging = paging;
      this.hotspots = hotspots;
    }

    boolean isPresent() {
      return !hotspots.isEmpty();
    }

    public Paging getPaging() {
      return paging;
    }

    List<IssueDto> getHotspots() {
      return hotspots;
    }

    void addComponents(Collection<ComponentDto> components) {
      for (ComponentDto component : components) {
        componentsByUuid.put(component.uuid(), component);
      }
    }

    public void addBranches(List<BranchDto> branchDtos) {
      for (BranchDto branch : branchDtos) {
        branchesByBranchUuid.put(branch.getUuid(), branch);
      }
    }

    public BranchDto getBranch(String branchUuid) {
      return branchesByBranchUuid.get(branchUuid);
    }

    Collection<ComponentDto> getComponents() {
      return componentsByUuid.values();
    }

    public Map<String, ComponentDto> getComponentsByUuid() {
      return componentsByUuid;
    }
  }

}
