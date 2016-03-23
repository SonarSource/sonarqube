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
package org.sonar.server.issue.ws;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.es.Facets;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.ws.WsResponseCommonFormat;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Strings.nullToEmpty;

public class SearchResponseFormat {

  private final Durations durations;
  private final WsResponseCommonFormat commonFormat;
  private final Languages languages;

  public SearchResponseFormat(Durations durations, WsResponseCommonFormat commonFormat, Languages languages) {
    this.durations = durations;
    this.commonFormat = commonFormat;
    this.languages = languages;
  }

  public Issues.SearchWsResponse formatSearch(Set<SearchAdditionalField> fields, SearchResponseData data,
    Paging paging, @Nullable Facets facets) {
    Issues.SearchWsResponse.Builder response = Issues.SearchWsResponse.newBuilder();

    formatPaging(paging, response);
    formatEffortTotal(data, response);
    response.addAllIssues(formatIssues(fields, data));
    response.addAllComponents(formatComponents(data));
    if (facets != null) {
      formatFacets(facets, response);
    }
    if (fields.contains(SearchAdditionalField.RULES)) {
      response.setRules(formatRules(data));
    }
    if (fields.contains(SearchAdditionalField.USERS)) {
      response.setUsers(formatUsers(data));
    }
    if (fields.contains(SearchAdditionalField.LANGUAGES)) {
      response.setLanguages(formatLanguages());
    }
    return response.build();
  }

  public Issues.Operation formatOperation(SearchResponseData data) {
    Issues.Operation.Builder response = Issues.Operation.newBuilder();

    if (data.getIssues().size() == 1) {
      Issues.Issue.Builder issueBuilder = Issues.Issue.newBuilder();
      IssueDto dto = data.getIssues().get(0);
      formatIssue(issueBuilder, dto, data);
      formatIssueActions(data, issueBuilder, dto);
      formatIssueTransitions(data, issueBuilder, dto);
      formatIssueComments(data, issueBuilder, dto);
      response.setIssue(issueBuilder.build());
    }
    response.addAllComponents(formatComponents(data));
    response.addAllRules(formatRules(data).getRulesList());
    response.addAllUsers(formatUsers(data).getUsersList());
    return response.build();
  }

  private void formatEffortTotal(SearchResponseData data, Issues.SearchWsResponse.Builder response) {
    Long effort = data.getEffortTotal();
    if (effort != null) {
      response.setDebtTotal(effort);
      response.setEffortTotal(effort);
    }
  }

  private void formatPaging(Paging paging, Issues.SearchWsResponse.Builder response) {
    response.setP(paging.pageIndex());
    response.setPs(paging.pageSize());
    response.setTotal(paging.total());
    response.setPaging(commonFormat.formatPaging(paging));
  }

  private List<Issues.Issue> formatIssues(Set<SearchAdditionalField> fields, SearchResponseData data) {
    List<Issues.Issue> result = new ArrayList<>();
    Issues.Issue.Builder issueBuilder = Issues.Issue.newBuilder();
    for (IssueDto dto : data.getIssues()) {
      issueBuilder.clear();
      formatIssue(issueBuilder, dto, data);
      if (fields.contains(SearchAdditionalField.ACTIONS)) {
        formatIssueActions(data, issueBuilder, dto);
      }
      if (fields.contains(SearchAdditionalField.TRANSITIONS)) {
        formatIssueTransitions(data, issueBuilder, dto);
      }
      if (fields.contains(SearchAdditionalField.COMMENTS)) {
        formatIssueComments(data, issueBuilder, dto);
      }
      result.add(issueBuilder.build());
    }
    return result;
  }

  private void formatIssue(Issues.Issue.Builder issueBuilder, IssueDto dto, SearchResponseData data) {
    issueBuilder.setKey(dto.getKey());
    Common.RuleType type = Common.RuleType.valueOf(dto.getType());
    if (type != null) {
      issueBuilder.setType(type);
    }
    ComponentDto component = data.getComponentByUuid(dto.getComponentUuid());
    issueBuilder.setComponent(component.key());
    // Only used for the compatibility with the Java WS Client <= 4.4 used by Eclipse
    issueBuilder.setComponentId(component.getId());
    ComponentDto project = data.getComponentByUuid(dto.getProjectUuid());
    if (project != null) {
      issueBuilder.setProject(project.getKey());
      ComponentDto subProject = data.getComponentByUuid(dto.getModuleUuid());
      if (subProject != null && !subProject.getKey().equals(project.getKey())) {
        issueBuilder.setSubProject(subProject.getKey());
      }
    }
    issueBuilder.setRule(dto.getRuleKey().toString());
    issueBuilder.setSeverity(Common.Severity.valueOf(dto.getSeverity()));
    if (!Strings.isNullOrEmpty(dto.getAssignee())) {
      issueBuilder.setAssignee(dto.getAssignee());
    }
    if (!Strings.isNullOrEmpty(dto.getResolution())) {
      issueBuilder.setResolution(dto.getResolution());
    }
    issueBuilder.setStatus(dto.getStatus());
    issueBuilder.setMessage(nullToEmpty(dto.getMessage()));
    issueBuilder.addAllTags(dto.getTags());
    Long effort = dto.getEffort();
    if (effort != null) {
      String effortValue = durations.encode(Duration.create(effort));
      issueBuilder.setDebt(effortValue);
      issueBuilder.setEffort(effortValue);
    }
    Integer line = dto.getLine();
    if (line != null) {
      issueBuilder.setLine(line);
    }
    completeIssueLocations(dto, issueBuilder);
    issueBuilder.setAuthor(nullToEmpty(dto.getAuthorLogin()));
    Date date = dto.getIssueCreationDate();
    if (date != null) {
      issueBuilder.setCreationDate(DateUtils.formatDateTime(date));
    }
    date = dto.getIssueUpdateDate();
    if (date != null) {
      issueBuilder.setUpdateDate(DateUtils.formatDateTime(date));
    }
    date = dto.getIssueCloseDate();
    if (date != null) {
      issueBuilder.setCloseDate(DateUtils.formatDateTime(date));
    }
  }

  private void completeIssueLocations(IssueDto dto, Issues.Issue.Builder issueBuilder) {
    DbIssues.Locations locations = dto.parseLocations();
    if (locations != null) {
      if (locations.hasTextRange()) {
        DbCommons.TextRange textRange = locations.getTextRange();
        issueBuilder.setTextRange(convertTextRange(textRange));
      }
      for (DbIssues.Flow flow : locations.getFlowList()) {
        Issues.Flow.Builder targetFlow = Issues.Flow.newBuilder();
        for (DbIssues.Location flowLocation : flow.getLocationList()) {
          targetFlow.addLocations(convertLocation(flowLocation));
        }
        issueBuilder.addFlows(targetFlow);
      }
    }
  }

  private static Issues.Location convertLocation(DbIssues.Location source) {
    Issues.Location.Builder target = Issues.Location.newBuilder();
    if (source.hasComponentId()) {
      target.setComponentId(source.getComponentId());
    }
    if (source.hasMsg()) {
      target.setMsg(source.getMsg());
    }
    if (source.hasTextRange()) {
      DbCommons.TextRange sourceRange = source.getTextRange();
      Common.TextRange.Builder targetRange = convertTextRange(sourceRange);
      target.setTextRange(targetRange);
    }
    return target.build();
  }

  private static Common.TextRange.Builder convertTextRange(DbCommons.TextRange sourceRange) {
    Common.TextRange.Builder targetRange = Common.TextRange.newBuilder();
    if (sourceRange.hasStartLine()) {
      targetRange.setStartLine(sourceRange.getStartLine());
    }
    if (sourceRange.hasStartOffset()) {
      targetRange.setStartOffset(sourceRange.getStartOffset());
    }
    if (sourceRange.hasEndLine()) {
      targetRange.setEndLine(sourceRange.getEndLine());
    }
    if (sourceRange.hasEndOffset()) {
      targetRange.setEndOffset(sourceRange.getEndOffset());
    }
    return targetRange;
  }

  private static void formatIssueTransitions(SearchResponseData data, Issues.Issue.Builder wsIssue, IssueDto dto) {
    Issues.Transitions.Builder wsTransitions = Issues.Transitions.newBuilder();
    List<Transition> transitions = data.getTransitionsForIssueKey(dto.getKey());
    if (transitions != null) {
      for (Transition transition : transitions) {
        wsTransitions.addTransitions(transition.key());
      }
    }
    wsIssue.setTransitions(wsTransitions);
  }

  private static void formatIssueActions(SearchResponseData data, Issues.Issue.Builder wsIssue, IssueDto dto) {
    Issues.Actions.Builder wsActions = Issues.Actions.newBuilder();
    List<String> actions = data.getActionsForIssueKey(dto.getKey());
    if (actions != null) {
      wsActions.addAllActions(actions);
    }
    wsIssue.setActions(wsActions);
  }

  private static void formatIssueComments(SearchResponseData data, Issues.Issue.Builder wsIssue, IssueDto dto) {
    Issues.Comments.Builder wsComments = Issues.Comments.newBuilder();
    List<IssueChangeDto> comments = data.getCommentsForIssueKey(dto.getKey());
    if (comments != null) {
      Issues.Comment.Builder wsComment = Issues.Comment.newBuilder();
      for (IssueChangeDto comment : comments) {
        String markdown = comment.getChangeData();
        wsComment
          .clear()
          .setKey(comment.getKey())
          .setLogin(nullToEmpty(comment.getUserLogin()))
          .setUpdatable(data.isUpdatableComment(comment.getKey()))
          .setCreatedAt(DateUtils.formatDateTime(new Date(comment.getCreatedAt())));
        if (markdown != null) {
          wsComment
            .setHtmlText(Markdown.convertToHtml(markdown))
            .setMarkdown(markdown);
        }
        wsComments.addComments(wsComment);
      }
    }
    wsIssue.setComments(wsComments);
  }

  private Common.Rules.Builder formatRules(SearchResponseData data) {
    Common.Rules.Builder wsRules = Common.Rules.newBuilder();
    List<RuleDto> rules = data.getRules();
    if (rules != null) {
      for (RuleDto rule : rules) {
        wsRules.addRules(commonFormat.formatRule(rule));
      }
    }
    return wsRules;
  }

  private static List<Issues.Component> formatComponents(SearchResponseData data) {
    List<Issues.Component> result = new ArrayList<>();
    Collection<ComponentDto> components = data.getComponents();
    if (components != null) {
      for (ComponentDto dto : components) {
        Issues.Component.Builder builder = Issues.Component.newBuilder()
          .setId(dto.getId())
          .setKey(dto.key())
          .setUuid(dto.uuid())
          .setQualifier(dto.qualifier())
          .setName(nullToEmpty(dto.name()))
          .setLongName(nullToEmpty(dto.longName()))
          .setEnabled(dto.isEnabled());
        String path = dto.path();
        // path is not applicable to the components that are not files.
        // Value must not be "" in this case.
        if (!Strings.isNullOrEmpty(path)) {
          builder.setPath(path);
        }

        // On a root project, parentProjectId is null but projectId is equal to itself, which make no sense.
        if (dto.projectUuid() != null && dto.parentProjectId() != null) {
          ComponentDto project = data.getComponentByUuid(dto.projectUuid());
          builder.setProjectId(project.getId());
        }
        if (dto.parentProjectId() != null) {
          builder.setSubProjectId(dto.parentProjectId());
        }
        result.add(builder.build());
      }
    }
    return result;
  }

  private Common.Users.Builder formatUsers(SearchResponseData data) {
    Common.Users.Builder wsUsers = Common.Users.newBuilder();
    List<UserDto> users = data.getUsers();
    if (users != null) {
      for (UserDto user : users) {
        wsUsers.addUsers(commonFormat.formatUser(user));
      }
    }
    return wsUsers;
  }

  private Issues.Languages.Builder formatLanguages() {
    Issues.Languages.Builder wsLangs = Issues.Languages.newBuilder();
    Issues.Language.Builder wsLang = Issues.Language.newBuilder();
    for (Language lang : languages.all()) {
      wsLang
        .clear()
        .setKey(lang.getKey())
        .setName(lang.getName());
      wsLangs.addLanguages(wsLang);
    }
    return wsLangs;
  }

  private void formatFacets(Facets facets, Issues.SearchWsResponse.Builder wsSearch) {
    Common.Facets.Builder wsFacets = Common.Facets.newBuilder();
    Common.Facet.Builder wsFacet = Common.Facet.newBuilder();
    for (Map.Entry<String, LinkedHashMap<String, Long>> facet : facets.getAll().entrySet()) {
      wsFacet.clear();
      wsFacet.setProperty(facet.getKey());
      LinkedHashMap<String, Long> buckets = facet.getValue();
      if (buckets != null) {
        for (Map.Entry<String, Long> bucket : buckets.entrySet()) {
          Common.FacetValue.Builder valueBuilder = wsFacet.addValuesBuilder();
          valueBuilder.setVal(bucket.getKey());
          valueBuilder.setCount(bucket.getValue());
          valueBuilder.build();
        }
      } else {
        wsFacet.addAllValues(Collections.<Common.FacetValue>emptyList());
      }
      wsFacets.addFacets(wsFacet);
    }
    wsSearch.setFacets(wsFacets);
  }
}
