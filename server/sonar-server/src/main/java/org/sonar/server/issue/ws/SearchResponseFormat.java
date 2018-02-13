/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static org.sonar.core.util.Protobuf.setNullable;

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

  public SearchWsResponse formatSearch(Set<SearchAdditionalField> fields, SearchResponseData data,
    Paging paging, @Nullable Facets facets) {
    SearchWsResponse.Builder response = SearchWsResponse.newBuilder();

    formatPaging(paging, response);
    formatEffortTotal(data, response);
    response.addAllIssues(formatIssues(fields, data));
    response.addAllComponents(formatComponents(data));
    if (facets != null) {
      formatFacets(facets, response);
    }
    if (fields.contains(SearchAdditionalField.RULE_IDS_AND_KEYS)) {
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

  public Operation formatOperation(SearchResponseData data) {
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

  private void formatEffortTotal(SearchResponseData data, SearchWsResponse.Builder response) {
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
    setNullable(dto.getType(), issueBuilder::setType, Common.RuleType::valueOf);

    ComponentDto component = data.getComponentByUuid(dto.getComponentUuid());
    issueBuilder.setOrganization(data.getOrganizationKey(component.getOrganizationUuid()));
    issueBuilder.setComponent(component.getKey());
    setNullable(component.getBranch(), issueBuilder::setBranch);
    ComponentDto project = data.getComponentByUuid(dto.getProjectUuid());
    if (project != null) {
      issueBuilder.setProject(project.getKey());
      ComponentDto subProject = data.getComponentByUuid(dto.getModuleUuid());
      if (subProject != null && !subProject.getDbKey().equals(project.getDbKey())) {
        issueBuilder.setSubProject(subProject.getKey());
      }
    }
    issueBuilder.setRule(dto.getRuleKey().toString());
    issueBuilder.setSeverity(Common.Severity.valueOf(dto.getSeverity()));
    setNullable(emptyToNull(dto.getAssignee()), issueBuilder::setAssignee);
    setNullable(emptyToNull(dto.getResolution()), issueBuilder::setResolution);
    issueBuilder.setStatus(dto.getStatus());
    issueBuilder.setMessage(nullToEmpty(dto.getMessage()));
    issueBuilder.addAllTags(dto.getTags());
    Long effort = dto.getEffort();
    if (effort != null) {
      String effortValue = durations.encode(Duration.create(effort));
      issueBuilder.setDebt(effortValue);
      issueBuilder.setEffort(effortValue);
    }
    setNullable(dto.getLine(), issueBuilder::setLine);
    setNullable(emptyToNull(dto.getChecksum()), issueBuilder::setHash);
    completeIssueLocations(dto, issueBuilder);
    issueBuilder.setAuthor(nullToEmpty(dto.getAuthorLogin()));
    setNullable(dto.getIssueCreationDate(), issueBuilder::setCreationDate, DateUtils::formatDateTime);
    setNullable(dto.getIssueUpdateDate(), issueBuilder::setUpdateDate, DateUtils::formatDateTime);
    setNullable(dto.getIssueCloseDate(), issueBuilder::setCloseDate, DateUtils::formatDateTime);
  }

  private static void completeIssueLocations(IssueDto dto, Issue.Builder issueBuilder) {
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
        targetFlow.addLocations(convertLocation(flowLocation));
      }
      issueBuilder.addFlows(targetFlow);
    }
  }

  private static Location convertLocation(DbIssues.Location source) {
    Location.Builder target = Location.newBuilder();
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
          .setLogin(nullToEmpty(comment.getUserLogin()))
          .setUpdatable(data.isUpdatableComment(comment.getKey()))
          .setCreatedAt(DateUtils.formatDateTime(new Date(comment.getIssueChangeCreationDate())));
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
    List<RuleDefinitionDto> rules = data.getRules();
    if (rules != null) {
      for (RuleDefinitionDto rule : rules) {
        wsRules.addRules(commonFormat.formatRule(rule));
      }
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
      setNullable(dto.getBranch(), builder::setBranch);
      String path = dto.path();
      // path is not applicable to the components that are not files.
      // Value must not be "" in this case.
      if (!Strings.isNullOrEmpty(path)) {
        builder.setPath(path);
      }

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
    setNullable(emptyToNull(user.getEmail()), email -> builder.setAvatar(avatarFactory.create(user)));
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

  private void formatFacets(Facets facets, SearchWsResponse.Builder wsSearch) {
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
        wsFacet.addAllValues(Collections.emptyList());
      }
      wsFacets.addFacets(wsFacet);
    }
    wsSearch.setFacets(wsFacets);
  }
}
