/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.core.issue.workflow.Transition;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.ActionPlanDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.es.Facets;
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

  public Issues.Search formatSearch(Set<SearchAdditionalField> fields, SearchResponseData data,
    Paging paging, @Nullable Facets facets) {
    Issues.Search.Builder response = Issues.Search.newBuilder();

    formatPaging(paging, response);
    formatDebtTotal(data, response);
    response.addAllIssues(formatIssues(fields, data));
    response.addAllComponents(formatComponents(data));
    if (facets != null) {
      formatFacets(facets, response);
    }
    if (fields.contains(SearchAdditionalField.RULES)) {
      response.setRulesPresentIfEmpty(true);
      response.addAllRules(formatRules(data));
    }
    if (fields.contains(SearchAdditionalField.USERS)) {
      response.setUsersPresentIfEmpty(true);
      response.addAllUsers(formatUsers(data));
    }
    if (fields.contains(SearchAdditionalField.ACTION_PLANS)) {
      response.setActionPlansPresentIfEmpty(true);
      response.addAllActionPlans(formatActionPlans(data));
    }
    if (fields.contains(SearchAdditionalField.LANGUAGES)) {
      response.setLanguagesPresentIfEmpty(true);
      response.addAllLanguages(formatLanguages());
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
    response.addAllRules(formatRules(data));
    response.addAllUsers(formatUsers(data));
    response.addAllActionPlans(formatActionPlans(data));
    return response.build();
  }

  private void formatDebtTotal(SearchResponseData data, Issues.Search.Builder response) {
    Long debt = data.getDebtTotal();
    if (debt != null) {
      response.setDebtTotal(debt);
    }
  }

  private void formatPaging(Paging paging, Issues.Search.Builder response) {
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
      // TODO attributes
      result.add(issueBuilder.build());
    }
    return result;
  }

  private void formatIssue(Issues.Issue.Builder issueBuilder, IssueDto dto, SearchResponseData data) {
    issueBuilder.setKey(dto.getKey());
    ComponentDto component = data.getComponentByUuid(dto.getComponentUuid());
    issueBuilder.setComponent(dto.getComponentKey());
    // Only used for the compatibility with the Java WS Client <= 4.4 used by Eclipse
    issueBuilder.setComponentId(component.getId());
    ComponentDto project = data.getComponentByUuid(dto.getProjectUuid());
    if (project != null) {
      issueBuilder.setProject(project.getKey());
    }
    ComponentDto subProject = data.getComponentByUuid(dto.getModuleUuid());
    if (subProject != null) {
      issueBuilder.setSubProject(subProject.getKey());
    }
    issueBuilder.setRule(dto.getRuleKey().toString());
    issueBuilder.setSeverity(Common.Severity.valueOf(dto.getSeverity()));
    if (!Strings.isNullOrEmpty(dto.getAssignee())) {
      issueBuilder.setAssignee(dto.getAssignee());
    }
    if (!Strings.isNullOrEmpty(dto.getReporter())) {
      issueBuilder.setReporter(dto.getReporter());
    }
    if (!Strings.isNullOrEmpty(dto.getResolution())) {
      issueBuilder.setResolution(dto.getResolution());
    }
    issueBuilder.setStatus(dto.getStatus());
    if (!Strings.isNullOrEmpty(dto.getActionPlanKey())) {
      issueBuilder.setActionPlan(dto.getActionPlanKey());
    }
    issueBuilder.setMessage(nullToEmpty(dto.getMessage()));
    issueBuilder.setTagsPresentIfEmpty(true);
    issueBuilder.addAllTags(dto.getTags());
    Long debt = dto.getDebt();
    if (debt != null) {
      issueBuilder.setDebt(durations.encode(Duration.create(debt)));
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
      if (locations.hasPrimary()) {
        DbCommons.TextRange primary = locations.getPrimary();
        issueBuilder.setTextRange(convertTextRange(primary));
      }
      for (DbIssues.Location secondary : locations.getSecondaryList()) {
        issueBuilder.addSecondaryLocations(convertLocation(secondary));
      }
      for (DbIssues.ExecutionFlow flow : locations.getExecutionFlowList()) {
        Issues.ExecutionFlow.Builder targetFlow = Issues.ExecutionFlow.newBuilder();
        for (DbIssues.Location flowLocation : flow.getLocationList()) {
          targetFlow.addLocations(convertLocation(flowLocation));
        }
        issueBuilder.addExecutionFlows(targetFlow);
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

  private static void formatIssueTransitions(SearchResponseData data, Issues.Issue.Builder issueBuilder, IssueDto dto) {
    issueBuilder.setTransitionsPresentIfEmpty(true);
    List<Transition> transitions = data.getTransitionsForIssueKey(dto.getKey());
    if (transitions != null) {
      for (Transition transition : transitions) {
        issueBuilder.addTransitions(transition.key());
      }
    }
  }

  private static void formatIssueActions(SearchResponseData data, Issues.Issue.Builder issueBuilder, IssueDto dto) {
    issueBuilder.setActionsPresentIfEmpty(true);
    List<String> actions = data.getActionsForIssueKey(dto.getKey());
    if (actions != null) {
      issueBuilder.addAllActions(actions);
    }
  }

  private static void formatIssueComments(SearchResponseData data, Issues.Issue.Builder issueBuilder, IssueDto dto) {
    issueBuilder.setCommentsPresentIfEmpty(true);
    List<IssueChangeDto> comments = data.getCommentsForIssueKey(dto.getKey());
    if (comments != null) {
      Issues.Comment.Builder commentBuilder = Issues.Comment.newBuilder();
      for (IssueChangeDto comment : comments) {
        String markdown = comment.getChangeData();
        commentBuilder
          .clear()
          .setKey(comment.getKey())
          .setLogin(nullToEmpty(comment.getUserLogin()))
          .setUpdatable(data.isUpdatableComment(comment.getKey()))
          .setCreatedAt(DateUtils.formatDateTime(new Date(comment.getCreatedAt())));
        if (markdown != null) {
          commentBuilder
            .setHtmlText(Markdown.convertToHtml(markdown))
            .setMarkdown(markdown);
        }
        issueBuilder.addComments(commentBuilder.build());
      }
    }
  }

  private List<Common.Rule> formatRules(SearchResponseData data) {
    List<Common.Rule> result = new ArrayList<>();
    List<RuleDto> rules = data.getRules();
    if (rules != null) {
      for (RuleDto rule : rules) {
        result.add(commonFormat.formatRule(rule).build());
      }
    }
    return result;
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

  private List<Common.User> formatUsers(SearchResponseData data) {
    List<Common.User> result = new ArrayList<>();
    List<UserDto> users = data.getUsers();
    if (users != null) {
      for (UserDto user : users) {
        result.add(commonFormat.formatUser(user).build());
      }
    }
    return result;
  }

  private List<Issues.ActionPlan> formatActionPlans(SearchResponseData data) {
    List<Issues.ActionPlan> result = new ArrayList<>();
    List<ActionPlanDto> actionPlans = data.getActionPlans();
    if (actionPlans != null) {
      Issues.ActionPlan.Builder planBuilder = Issues.ActionPlan.newBuilder();
      for (ActionPlanDto actionPlan : actionPlans) {
        planBuilder
          .clear()
          .setKey(actionPlan.getKey())
          .setName(nullToEmpty(actionPlan.getName()))
          .setStatus(nullToEmpty(actionPlan.getStatus()))
          .setProject(nullToEmpty(actionPlan.getProjectKey()));
        Date deadLine = actionPlan.getDeadLine();
        if (deadLine != null) {
          planBuilder.setDeadLine(DateUtils.formatDateTime(deadLine));
        }
        result.add(planBuilder.build());
      }
    }
    return result;
  }

  private List<Issues.Language> formatLanguages() {
    List<Issues.Language> result = new ArrayList<>();
    Issues.Language.Builder builder = Issues.Language.newBuilder();
    for (Language lang : languages.all()) {
      builder
        .clear()
        .setKey(lang.getKey())
        .setName(lang.getName());
      result.add(builder.build());
    }
    return result;
  }

  private void formatFacets(Facets facets, Issues.Search.Builder response) {
    response.setFacetsPresentIfEmpty(true);
    Common.Facet.Builder facetBuilder = Common.Facet.newBuilder();
    for (Map.Entry<String, LinkedHashMap<String, Long>> facet : facets.getAll().entrySet()) {
      facetBuilder.clear();
      facetBuilder.setProperty(facet.getKey());
      LinkedHashMap<String, Long> buckets = facet.getValue();
      if (buckets != null) {
        for (Map.Entry<String, Long> bucket : buckets.entrySet()) {
          Common.FacetValue.Builder valueBuilder = facetBuilder.addValuesBuilder();
          valueBuilder.setVal(bucket.getKey());
          valueBuilder.setCount(bucket.getValue());
          valueBuilder.build();
        }
      } else {
        facetBuilder.addAllValues(Collections.<Common.FacetValue>emptyList());
      }
      response.addFacets(facetBuilder.build());
    }
  }
}
