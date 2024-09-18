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
package org.sonar.server.issue.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.es.Facets;
import org.sonar.server.issue.ImpactFormatter;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.index.IssueScope;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.Comment;
import org.sonarqube.ws.Common.User;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Actions;
import org.sonarqube.ws.Issues.Comments;
import org.sonarqube.ws.Issues.Component;
import org.sonarqube.ws.Issues.Issue;
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
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.api.rule.RuleKey.EXTERNAL_RULE_REPO_PREFIX;
import static org.sonar.server.issue.index.IssueIndex.FACET_ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.IssueIndex.FACET_PROJECTS;
import static org.sonar.server.issue.ws.SearchAdditionalField.ACTIONS;
import static org.sonar.server.issue.ws.SearchAdditionalField.ALL_ADDITIONAL_FIELDS;
import static org.sonar.server.issue.ws.SearchAdditionalField.COMMENTS;
import static org.sonar.server.issue.ws.SearchAdditionalField.RULE_DESCRIPTION_CONTEXT_KEY;
import static org.sonar.server.issue.ws.SearchAdditionalField.TRANSITIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;

public class SearchResponseFormat {

  private final Durations durations;
  private final Languages languages;
  private final TextRangeResponseFormatter textRangeFormatter;
  private final UserResponseFormatter userFormatter;

  public SearchResponseFormat(Durations durations, Languages languages, TextRangeResponseFormatter textRangeFormatter, UserResponseFormatter userFormatter) {
    this.durations = durations;
    this.languages = languages;
    this.textRangeFormatter = textRangeFormatter;
    this.userFormatter = userFormatter;
  }

  SearchWsResponse formatSearch(Set<SearchAdditionalField> fields, SearchResponseData data, Paging paging, Facets facets) {
    SearchWsResponse.Builder response = SearchWsResponse.newBuilder();

    formatPaging(paging, response);
    ofNullable(data.getEffortTotal()).ifPresent(response::setEffortTotal);
    response.addAllIssues(createIssues(fields, data));
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

  Issues.ListWsResponse formatList(Set<SearchAdditionalField> fields, SearchResponseData data, Paging paging) {
    Issues.ListWsResponse.Builder response = Issues.ListWsResponse.newBuilder();

    response.setPaging(Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(data.getIssues().size()));
    response.addAllIssues(createIssues(fields, data));
    response.addAllComponents(formatComponents(data));
    return response.build();
  }

  Operation formatOperation(SearchResponseData data) {
    Operation.Builder response = Operation.newBuilder();

    if (data.getIssues().size() == 1) {
      IssueDto dto = data.getIssues().get(0);
      response.setIssue(createIssue(ALL_ADDITIONAL_FIELDS, data, dto));
    }
    response.addAllComponents(formatComponents(data));
    response.addAllRules(formatRules(data).getRulesList());
    response.addAllUsers(formatUsers(data).getUsersList());
    return response.build();
  }

  private static void formatPaging(Paging paging, SearchWsResponse.Builder response) {
    response.setP(paging.pageIndex());
    response.setPs(paging.pageSize());
    response.setTotal(paging.total());
    response.setPaging(formatPaging(paging));
  }

  private static Common.Paging.Builder formatPaging(Paging paging) {
    return Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());
  }

  private List<Issues.Issue> createIssues(Collection<SearchAdditionalField> fields, SearchResponseData data) {
    return data.getIssues().stream()
      .map(dto -> createIssue(fields, data, dto))
      .toList();
  }

  private Issue createIssue(Collection<SearchAdditionalField> fields, SearchResponseData data, IssueDto dto) {
    Issue.Builder issueBuilder = Issue.newBuilder();
    addMandatoryFieldsToIssueBuilder(issueBuilder, dto, data);
    addAdditionalFieldsToIssueBuilder(fields, data, dto, issueBuilder);
    return issueBuilder.build();
  }

  private void addMandatoryFieldsToIssueBuilder(Issue.Builder issueBuilder, IssueDto dto, SearchResponseData data) {
    issueBuilder.setKey(dto.getKey());
    issueBuilder.setType(Common.RuleType.forNumber(dto.getType()));

    CleanCodeAttribute cleanCodeAttribute = dto.getEffectiveCleanCodeAttribute();
    if (cleanCodeAttribute != null) {
      issueBuilder.setCleanCodeAttribute(Common.CleanCodeAttribute.valueOf(cleanCodeAttribute.name()));
      issueBuilder.setCleanCodeAttributeCategory(Common.CleanCodeAttributeCategory.valueOf(cleanCodeAttribute.getAttributeCategory().name()));
    }
    issueBuilder.addAllImpacts(dto.getEffectiveImpacts().entrySet()
      .stream()
      .map(entry -> Common.Impact.newBuilder()
        .setSoftwareQuality(Common.SoftwareQuality.valueOf(entry.getKey().name()))
        .setSeverity(ImpactFormatter.mapImpactSeverity(entry.getValue()))
        .build())
      .toList());

    ComponentDto component = data.getComponentByUuid(dto.getComponentUuid());
    issueBuilder.setComponent(component.getKey());
    setBranchOrPr(component, issueBuilder, data);
    ComponentDto branch = data.getComponentByUuid(dto.getProjectUuid());
    if (branch != null) {
      issueBuilder.setProject(branch.getKey());
    }
    issueBuilder.setRule(dto.getRuleKey().toString());
    if (dto.isExternal()) {
      issueBuilder.setExternalRuleEngine(engineNameFrom(dto.getRuleKey()));
    }
    if (dto.getType() != RuleType.SECURITY_HOTSPOT.getDbConstant()) {
      issueBuilder.setSeverity(Common.Severity.valueOf(dto.getSeverity()));
    }
    ofNullable(data.getUserByUuid(dto.getAssigneeUuid())).ifPresent(assignee -> issueBuilder.setAssignee(assignee.getLogin()));
    ofNullable(emptyToNull(dto.getResolution())).ifPresent(issueBuilder::setResolution);
    issueBuilder.setStatus(dto.getStatus());
    ofNullable(dto.getIssueStatus()).map(IssueStatus::name).ifPresent(issueBuilder::setIssueStatus);
    issueBuilder.setMessage(nullToEmpty(dto.getMessage()));
    issueBuilder.addAllMessageFormattings(MessageFormattingUtils.dbMessageFormattingToWs(dto.parseMessageFormattings()));
    issueBuilder.addAllTags(dto.getTags());
    issueBuilder.addAllCodeVariants(dto.getCodeVariants());
    Long effort = dto.getEffort();
    if (effort != null) {
      String effortValue = durations.encode(Duration.create(effort));
      issueBuilder.setDebt(effortValue);
      issueBuilder.setEffort(effortValue);
    }
    ofNullable(dto.getLine()).ifPresent(issueBuilder::setLine);
    ofNullable(emptyToNull(dto.getChecksum())).ifPresent(issueBuilder::setHash);
    completeIssueLocations(dto, issueBuilder, data);

    issueBuilder.setAuthor(nullToEmpty(dto.getAuthorLogin()));
    ofNullable(dto.getIssueCreationDate()).map(DateUtils::formatDateTime).ifPresent(issueBuilder::setCreationDate);
    ofNullable(dto.getIssueUpdateDate()).map(DateUtils::formatDateTime).ifPresent(issueBuilder::setUpdateDate);
    ofNullable(dto.getIssueCloseDate()).map(DateUtils::formatDateTime).ifPresent(issueBuilder::setCloseDate);

    Optional.of(dto.isQuickFixAvailable())
      .ifPresentOrElse(issueBuilder::setQuickFixAvailable, () -> issueBuilder.setQuickFixAvailable(false));

    issueBuilder.setScope(UNIT_TEST_FILE.equals(component.qualifier()) ? IssueScope.TEST.name() : IssueScope.MAIN.name());
    issueBuilder.setPrioritizedRule(dto.isPrioritizedRule());

    Optional.ofNullable(dto.getCveId()).ifPresent(issueBuilder::setCveId);
  }

  private static void addAdditionalFieldsToIssueBuilder(Collection<SearchAdditionalField> fields, SearchResponseData data, IssueDto dto, Issue.Builder issueBuilder) {
    if (fields.contains(ACTIONS)) {
      issueBuilder.setActions(createIssueActions(data, dto));
    }
    if (fields.contains(TRANSITIONS)) {
      issueBuilder.setTransitions(createIssueTransition(data, dto));
    }
    if (fields.contains(COMMENTS)) {
      issueBuilder.setComments(createIssueComments(data, dto));
    }
    if (fields.contains(RULE_DESCRIPTION_CONTEXT_KEY)) {
      dto.getOptionalRuleDescriptionContextKey().ifPresent(issueBuilder::setRuleDescriptionContextKey);
    }
  }

  private static String engineNameFrom(RuleKey ruleKey) {
    checkState(ruleKey.repository().startsWith(EXTERNAL_RULE_REPO_PREFIX));
    return ruleKey.repository().replace(EXTERNAL_RULE_REPO_PREFIX, "");
  }

  private void completeIssueLocations(IssueDto dto, Issue.Builder issueBuilder, SearchResponseData data) {
    DbIssues.Locations locations = dto.parseLocations();
    if (locations == null) {
      return;
    }
    textRangeFormatter.formatTextRange(locations, issueBuilder::setTextRange);
    issueBuilder.addAllFlows(textRangeFormatter.formatFlows(locations, issueBuilder.getComponent(), data.getComponentsByUuid()));
  }

  private static Transitions createIssueTransition(SearchResponseData data, IssueDto dto) {
    Transitions.Builder wsTransitions = Transitions.newBuilder();
    List<Transition> transitions = data.getTransitionsForIssueKey(dto.getKey());
    if (transitions != null) {
      for (Transition transition : transitions) {
        wsTransitions.addTransitions(transition.key());
      }
    }
    return wsTransitions.build();
  }

  private static Actions createIssueActions(SearchResponseData data, IssueDto dto) {
    Actions.Builder wsActions = Actions.newBuilder();
    List<String> actions = data.getActionsForIssueKey(dto.getKey());
    if (actions != null) {
      wsActions.addAllActions(actions);
    }
    return wsActions.build();
  }

  private static Comments createIssueComments(SearchResponseData data, IssueDto dto) {
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
    return wsComments.build();
  }

  private Common.Rules.Builder formatRules(SearchResponseData data) {
    Common.Rules.Builder wsRules = Common.Rules.newBuilder();
    List<RuleDto> rules = firstNonNull(data.getRules(), emptyList());
    for (RuleDto rule : rules) {
      wsRules.addRules(formatRule(rule));
    }
    return wsRules;
  }

  private Common.Rule.Builder formatRule(RuleDto rule) {
    Common.Rule.Builder builder = Common.Rule.newBuilder()
      .setKey(rule.getKey().toString())
      .setName(nullToEmpty(rule.getName()))
      .setStatus(Common.RuleStatus.valueOf(rule.getStatus().name()));

    builder.setLang(nullToEmpty(rule.getLanguage()));
    Language lang = languages.get(rule.getLanguage());
    if (lang != null) {
      builder.setLangName(lang.getName());
    }
    return builder;
  }

  private static List<Issues.Component> formatComponents(SearchResponseData data) {
    Collection<ComponentDto> components = data.getComponents();
    List<Issues.Component> result = new ArrayList<>();
    for (ComponentDto dto : components) {
      Component.Builder builder = Component.newBuilder()
        .setKey(dto.getKey())
        .setQualifier(dto.qualifier())
        .setName(nullToEmpty(dto.name()))
        .setLongName(nullToEmpty(dto.longName()))
        .setEnabled(dto.isEnabled());
      setBranchOrPr(dto, builder, data);
      ofNullable(emptyToNull(dto.path())).ifPresent(builder::setPath);

      result.add(builder.build());
    }
    return result;
  }

  private static void setBranchOrPr(ComponentDto componentDto, Component.Builder builder, SearchResponseData data) {
    String branchUuid = componentDto.getCopyComponentUuid() != null ? componentDto.getCopyComponentUuid() : componentDto.branchUuid();
    BranchDto branchDto = data.getBranch(branchUuid);
    if (branchDto.isMain()) {
      return;
    }
    if (branchDto.getBranchType() == BranchType.BRANCH) {
      builder.setBranch(branchDto.getKey());
    } else if (branchDto.getBranchType() == BranchType.PULL_REQUEST) {
      builder.setPullRequest(branchDto.getKey());
    }
  }

  private static void setBranchOrPr(ComponentDto componentDto, Issue.Builder builder, SearchResponseData data) {
    String branchUuid = componentDto.getCopyComponentUuid() != null ? componentDto.getCopyComponentUuid() : componentDto.branchUuid();
    BranchDto branchDto = data.getBranch(branchUuid);
    if (branchDto.isMain()) {
      return;
    }
    if (branchDto.getBranchType() == BranchType.BRANCH) {
      builder.setBranch(branchDto.getKey());
    } else if (branchDto.getBranchType() == BranchType.PULL_REQUEST) {
      builder.setPullRequest(branchDto.getKey());
    }
  }

  private Users.Builder formatUsers(SearchResponseData data) {
    Users.Builder wsUsers = Users.newBuilder();
    List<UserDto> users = data.getUsers();
    if (users != null) {
      User.Builder builder = User.newBuilder();
      for (UserDto user : users) {
        wsUsers.addUsers(userFormatter.formatUser(builder, user));
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

    Map<String, RuleKey> ruleUuidsByRuleKeys = data.getRules().stream().collect(Collectors.toMap(RuleDto::getUuid, RuleDto::getKey));
    Common.Facet.Builder wsFacet = wsFacets.addFacetsBuilder();
    wsFacet.setProperty(PARAM_RULES);
    facet.forEach((ruleUuid, count) -> wsFacet.addValuesBuilder()
      .setVal(ruleUuidsByRuleKeys.get(ruleUuid).toString())
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
      ProjectDto project = datas.getProject(uuid);
      requireNonNull(project, format("Project has not been found for uuid '%s'", uuid));
      wsFacet.addValuesBuilder()
        .setVal(project.getKey())
        .setCount(count)
        .build();
    });
    wsFacet.build();
  }

}
