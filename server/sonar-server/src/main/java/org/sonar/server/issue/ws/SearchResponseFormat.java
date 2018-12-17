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
package org.sonar.server.issue.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.es.Facets;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.ws.WsResponseCommonFormat;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Actions;
import org.sonarqube.ws.Issues.Comment;
import org.sonarqube.ws.Issues.Comments;
import org.sonarqube.ws.Issues.Component;
import org.sonarqube.ws.Issues.Flow;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.Location;
import org.sonarqube.ws.Issues.Operation;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.Issues.Transitions;
import org.sonarqube.ws.Issues.Users;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.rule.RuleKey.EXTERNAL_RULE_REPO_PREFIX;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.issue.index.IssueIndex.FACET_ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.IssueIndex.FACET_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;

public class SearchResponseFormat {

  private final Durations durations;
  private final WsResponseCommonFormat commonFormat;
  private final Languages languages;
  private final AvatarResolver avatarFactory;

  public SearchResponseFormat(Durations durations, WsResponseCommonFormat commonFormat, Languages languages, AvatarResolver avatarFactory) {
    this.durations = durations;
    this.commonFormat = commonFormat;
    this.languages = languages;
    this.avatarFactory = avatarFactory;
  }

  SearchWsResponse formatSearch(Set<SearchAdditionalField> fields, SearchResponseData data, Paging paging, Facets facets) {
    SearchWsResponse.Builder response = SearchWsResponse.newBuilder();

    formatPaging(paging, response);
    formatEffortTotal(data, response);
    response.addAllIssues(formatIssues(fields, data));
    response.addAllComponents(formatComponents(data));
    formatFacets(data, facets, response);
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

  Operation formatOperation(SearchResponseData data) {
    Operation.Builder response = Operation.newBuilder();

    if (data.getIssues().size() == 1) {
      Issue.Builder issueBuilder = Issue.newBuilder();
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

  private static void formatEffortTotal(SearchResponseData data, SearchWsResponse.Builder response) {
    Long effort = data.getEffortTotal();
    if (effort != null) {
      response.setDebtTotal(effort);
      response.setEffortTotal(effort);
    }
  }

  private void formatPaging(Paging paging, SearchWsResponse.Builder response) {
    response.setP(paging.pageIndex());
    response.setPs(paging.pageSize());
    response.setTotal(paging.total());
    response.setPaging(commonFormat.formatPaging(paging));
  }

  private List<Issues.Issue> formatIssues(Set<SearchAdditionalField> fields, SearchResponseData data) {
    List<Issues.Issue> result = new ArrayList<>();
    Issue.Builder issueBuilder = Issue.newBuilder();
    data.getIssues().forEach(dto -> {
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
    });
    return result;
  }

  private void formatIssue(Issue.Builder issueBuilder, IssueDto dto, SearchResponseData data) {
    issueBuilder.setKey(dto.getKey());
    ofNullable(dto.getType()).map(Common.RuleType::forNumber).ifPresent(issueBuilder::setType);

    ComponentDto component = data.getComponentByUuid(dto.getComponentUuid());
    issueBuilder.setOrganization(data.getOrganizationKey(component.getOrganizationUuid()));
    issueBuilder.setComponent(component.getKey());
    ofNullable(component.getBranch()).ifPresent(issueBuilder::setBranch);
    ofNullable(component.getPullRequest()).ifPresent(issueBuilder::setPullRequest);
    ComponentDto project = data.getComponentByUuid(dto.getProjectUuid());
    if (project != null) {
      issueBuilder.setProject(project.getKey());
      ComponentDto subProject = data.getComponentByUuid(dto.getModuleUuid());
      if (subProject != null && !subProject.getDbKey().equals(project.getDbKey())) {
        issueBuilder.setSubProject(subProject.getKey());
      }
    }
    issueBuilder.setRule(dto.getRuleKey().toString());
    if (dto.isExternal()) {
      issueBuilder.setExternalRuleEngine(engineNameFrom(dto.getRuleKey()));
    }
    issueBuilder.setFromHotspot(dto.isFromHotspot());
    issueBuilder.setSeverity(Common.Severity.valueOf(dto.getSeverity()));
    ofNullable(data.getUserByUuid(dto.getAssigneeUuid())).ifPresent(assignee -> issueBuilder.setAssignee(assignee.getLogin()));
    ofNullable(emptyToNull(dto.getResolution())).ifPresent(issueBuilder::setResolution);
    issueBuilder.setStatus(dto.getStatus());
    issueBuilder.setMessage(nullToEmpty(dto.getMessage()));
    issueBuilder.addAllTags(dto.getTags());
    Long effort = dto.getEffort();
    if (effort != null) {
      String effortValue = durations.encode(Duration.create(effort));
      issueBuilder.setDebt(effortValue);
      issueBuilder.setEffort(effortValue);
    }
    ofNullable(dto.getLine()).ifPresent(issueBuilder::setLine);
    ofNullable(emptyToNull(dto.getChecksum())).ifPresent(issueBuilder::setHash);
    completeIssueLocations(dto, issueBuilder, data);

    // Filter author only if user is member of the organization
    if (data.getUserOrganizationUuids().contains(component.getOrganizationUuid())) {
      issueBuilder.setAuthor(nullToEmpty(dto.getAuthorLogin()));
    }
    ofNullable(dto.getIssueCreationDate()).map(DateUtils::formatDateTime).ifPresent(issueBuilder::setCreationDate);
    ofNullable(dto.getIssueUpdateDate()).map(DateUtils::formatDateTime).ifPresent(issueBuilder::setUpdateDate);
    ofNullable(dto.getIssueCloseDate()).map(DateUtils::formatDateTime).ifPresent(issueBuilder::setCloseDate);
  }

  private static String engineNameFrom(RuleKey ruleKey) {
    checkState(ruleKey.repository().startsWith(EXTERNAL_RULE_REPO_PREFIX));
    return ruleKey.repository().replace(EXTERNAL_RULE_REPO_PREFIX, "");
  }

  private static void completeIssueLocations(IssueDto dto, Issue.Builder issueBuilder, SearchResponseData data) {
    DbIssues.Locations locations = dto.parseLocations();
    if (locations == null) {
      return;
    }
    if (locations.hasTextRange()) {
      DbCommons.TextRange textRange = locations.getTextRange();
      issueBuilder.setTextRange(convertTextRange(textRange));
    }
    for (DbIssues.Flow flow : locations.getFlowList()) {
      Flow.Builder targetFlow = Flow.newBuilder();
      for (DbIssues.Location flowLocation : flow.getLocationList()) {
        targetFlow.addLocations(convertLocation(issueBuilder, flowLocation, data));
      }
      issueBuilder.addFlows(targetFlow);
    }
  }

  private static Location convertLocation(Issue.Builder issueBuilder, DbIssues.Location source, SearchResponseData data) {
    Location.Builder target = Location.newBuilder();
    if (source.hasMsg()) {
      target.setMsg(source.getMsg());
    }
    if (source.hasTextRange()) {
      DbCommons.TextRange sourceRange = source.getTextRange();
      Common.TextRange.Builder targetRange = convertTextRange(sourceRange);
      target.setTextRange(targetRange);
    }
    if (source.hasComponentId()) {
      ofNullable(data.getComponentByUuid(source.getComponentId())).ifPresent(c -> target.setComponent(c.getKey()));
    } else {
      target.setComponent(issueBuilder.getComponent());
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

  private static void formatIssueTransitions(SearchResponseData data, Issue.Builder wsIssue, IssueDto dto) {
    Transitions.Builder wsTransitions = Transitions.newBuilder();
    List<Transition> transitions = data.getTransitionsForIssueKey(dto.getKey());
    if (transitions != null) {
      for (Transition transition : transitions) {
        wsTransitions.addTransitions(transition.key());
      }
    }
    wsIssue.setTransitions(wsTransitions);
  }

  private static void formatIssueActions(SearchResponseData data, Issue.Builder wsIssue, IssueDto dto) {
    Actions.Builder wsActions = Actions.newBuilder();
    List<String> actions = data.getActionsForIssueKey(dto.getKey());
    if (actions != null) {
      wsActions.addAllActions(actions);
    }
    wsIssue.setActions(wsActions);
  }

  private static void formatIssueComments(SearchResponseData data, Issue.Builder wsIssue, IssueDto dto) {
    Comments.Builder wsComments = Comments.newBuilder();
    List<IssueChangeDto> comments = data.getCommentsForIssueKey(dto.getKey());
    if (comments != null) {
      Comment.Builder wsComment = Comment.newBuilder();
      for (IssueChangeDto comment : comments) {
        String markdown = comment.getChangeData();
        wsComment
          .clear()
          .setKey(comment.getKey())
          .setUpdatable(data.isUpdatableComment(comment.getKey()))
          .setCreatedAt(DateUtils.formatDateTime(new Date(comment.getIssueChangeCreationDate())));
        ofNullable(data.getUserByUuid(comment.getUserUuid())).ifPresent(user -> wsComment.setLogin(user.getLogin()));
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
    List<RuleDefinitionDto> rules = firstNonNull(data.getRules(), emptyList());
    for (RuleDefinitionDto rule : rules) {
      wsRules.addRules(commonFormat.formatRule(rule));
    }
    return wsRules;
  }

  private static List<Issues.Component> formatComponents(SearchResponseData data) {
    Collection<ComponentDto> components = data.getComponents();
    List<Issues.Component> result = new ArrayList<>();
    for (ComponentDto dto : components) {
      String uuid = dto.uuid();
      Component.Builder builder = Component.newBuilder()
        .setOrganization(data.getOrganizationKey(dto.getOrganizationUuid()))
        .setKey(dto.getKey())
        .setUuid(uuid)
        .setQualifier(dto.qualifier())
        .setName(nullToEmpty(dto.name()))
        .setLongName(nullToEmpty(dto.longName()))
        .setEnabled(dto.isEnabled());
      ofNullable(dto.getBranch()).ifPresent(builder::setBranch);
      ofNullable(dto.getPullRequest()).ifPresent(builder::setPullRequest);
      ofNullable(emptyToNull(dto.path())).ifPresent(builder::setPath);

      result.add(builder.build());
    }
    return result;
  }

  private Users.Builder formatUsers(SearchResponseData data) {
    Users.Builder wsUsers = Users.newBuilder();
    List<UserDto> users = data.getUsers();
    if (users != null) {
      for (UserDto user : users) {
        wsUsers.addUsers(formatUser(user));
      }
    }
    return wsUsers;
  }

  private Users.User.Builder formatUser(UserDto user) {
    Users.User.Builder builder = Users.User.newBuilder()
      .setLogin(user.getLogin())
      .setName(nullToEmpty(user.getName()))
      .setActive(user.isActive());
    ofNullable(emptyToNull(user.getEmail())).ifPresent(email -> builder.setAvatar(avatarFactory.create(user)));
    return builder;
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

  private static void formatFacets(SearchResponseData data, Facets facets, SearchWsResponse.Builder wsSearch) {
    Common.Facets.Builder wsFacets = Common.Facets.newBuilder();
    SearchAction.SUPPORTED_FACETS.stream()
      .filter(f -> !f.equals(FACET_PROJECTS))
      .filter(f -> !f.equals(FACET_ASSIGNED_TO_ME))
      .filter(f -> !f.equals(PARAM_ASSIGNEES))
      .filter(f -> !f.equals(PARAM_RULES))
      .forEach(f -> computeStandardFacet(wsFacets, facets, f));
    computeAssigneesFacet(wsFacets, facets, data);
    computeAssignedToMeFacet(wsFacets, facets, data);
    computeRulesFacet(wsFacets, facets, data);
    computeProjectsFacet(wsFacets, facets, data);
    wsSearch.setFacets(wsFacets.build());
  }

  private static void computeStandardFacet(Common.Facets.Builder wsFacets, Facets facets, String facetKey) {
    LinkedHashMap<String, Long> facet = facets.get(facetKey);
    if (facet == null) {
      return;
    }
    Common.Facet.Builder wsFacet = wsFacets.addFacetsBuilder();
    wsFacet.setProperty(facetKey);
    facet.forEach((value, count) -> wsFacet.addValuesBuilder()
      .setVal(value)
      .setCount(count)
      .build());
    wsFacet.build();
  }

  private static void computeAssigneesFacet(Common.Facets.Builder wsFacets, Facets facets, SearchResponseData data) {
    LinkedHashMap<String, Long> facet = facets.get(PARAM_ASSIGNEES);
    if (facet == null) {
      return;
    }
    Common.Facet.Builder wsFacet = wsFacets.addFacetsBuilder();
    wsFacet.setProperty(PARAM_ASSIGNEES);
    facet
      .forEach((userUuid, count) -> {
        UserDto user = data.getUserByUuid(userUuid);
        wsFacet.addValuesBuilder()
          .setVal(user == null ? "" : user.getLogin())
          .setCount(count)
          .build();
      });
    wsFacet.build();
  }

  private static void computeAssignedToMeFacet(Common.Facets.Builder wsFacets, Facets facets, SearchResponseData data) {
    LinkedHashMap<String, Long> facet = facets.get(FACET_ASSIGNED_TO_ME);
    if (facet == null) {
      return;
    }
    Map.Entry<String, Long> entry = facet.entrySet().iterator().next();
    UserDto user = data.getUserByUuid(entry.getKey());
    checkState(user != null, "User with uuid '%s' has not been found", entry.getKey());

    Common.Facet.Builder wsFacet = wsFacets.addFacetsBuilder();
    wsFacet.setProperty(FACET_ASSIGNED_TO_ME);
    wsFacet.addValuesBuilder()
      .setVal(user.getLogin())
      .setCount(entry.getValue())
      .build();
  }

  private static void computeRulesFacet(Common.Facets.Builder wsFacets, Facets facets, SearchResponseData data) {
    LinkedHashMap<String, Long> facet = facets.get(PARAM_RULES);
    if (facet == null) {
      return;
    }

    Map<Integer, RuleKey> ruleIdsByRuleKeys = data.getRules().stream().collect(uniqueIndex(RuleDefinitionDto::getId, RuleDefinitionDto::getKey));
    Common.Facet.Builder wsFacet = wsFacets.addFacetsBuilder();
    wsFacet.setProperty(PARAM_RULES);
    facet.forEach((ruleId, count) -> wsFacet.addValuesBuilder()
      .setVal(ruleIdsByRuleKeys.get(Integer.parseInt(ruleId)).toString())
      .setCount(count)
      .build());
    wsFacet.build();
  }

  private static void computeProjectsFacet(Common.Facets.Builder wsFacets, Facets facets, SearchResponseData datas) {
    LinkedHashMap<String, Long> facet = facets.get(FACET_PROJECTS);
    if (facet == null) {
      return;
    }
    Common.Facet.Builder wsFacet = wsFacets.addFacetsBuilder();
    wsFacet.setProperty(FACET_PROJECTS);
    facet.forEach((uuid, count) -> {
      ComponentDto component = datas.getComponentByUuid(uuid);
      requireNonNull(component, format("Component has not been found for uuid '%s'", uuid));
      wsFacet.addValuesBuilder()
        .setVal(component.getKey())
        .setCount(count)
        .build();
    });
    wsFacet.build();
  }

}
