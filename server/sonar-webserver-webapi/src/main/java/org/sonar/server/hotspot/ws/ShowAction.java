/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.protobuf.DbIssues.Locations;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.component.ComponentFinder.ProjectAndBranch;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueChangeWSSupport;
import org.sonar.server.issue.IssueChangeWSSupport.FormattingContext;
import org.sonar.server.issue.IssueChangeWSSupport.Load;
import org.sonar.server.issue.ws.UserResponseFormatter;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.ShowWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.difference;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ShowAction implements HotspotsWsAction {

  private static final String PARAM_HOTSPOT_KEY = "hotspot";

  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;
  private final HotspotWsResponseFormatter responseFormatter;
  private final UserResponseFormatter userFormatter;
  private final IssueChangeWSSupport issueChangeSupport;

  public ShowAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport, HotspotWsResponseFormatter responseFormatter,
    UserResponseFormatter userFormatter, IssueChangeWSSupport issueChangeSupport) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.responseFormatter = responseFormatter;
    this.userFormatter = userFormatter;
    this.issueChangeSupport = issueChangeSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setHandler(this)
      .setDescription("Provides the details of a Security Hotspot.")
      .setSince("8.1")
      .setChangelog(
        new Change("10.1", "Add the 'codeVariants' response field"),
        new Change("9.5", "The fields rule.riskDescription, rule.fixRecommendations, rule.vulnerabilityDescription of the response are deprecated."
          + " /api/rules/show endpoint should be used to fetch rule descriptions."),
        new Change("9.7", "Hotspot flows in the response may contain a description and a type"),
        new Change("9.8", "Add message formatting to issue and locations response"));

    action.createParam(PARAM_HOTSPOT_KEY)
      .setDescription("Key of the Security Hotspot")
      .setExampleValue(Uuids.UUID_EXAMPLE_03)
      .setRequired(true);

    action.setResponseExample(getClass().getResource("show-example.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String hotspotKey = request.mandatoryParam(PARAM_HOTSPOT_KEY);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto hotspot = hotspotWsSupport.loadHotspot(dbSession, hotspotKey);

      Components components = loadComponents(dbSession, hotspot);
      Users users = loadUsers(dbSession, hotspot);
      RuleDto rule = loadRule(dbSession, hotspot);

      ShowWsResponse.Builder responseBuilder = ShowWsResponse.newBuilder();
      formatHotspot(responseBuilder, hotspot, users);
      formatComponents(components, responseBuilder);
      formatRule(responseBuilder, rule);
      formatTextRange(responseBuilder, hotspot);
      formatFlows(dbSession, responseBuilder, hotspot);
      FormattingContext formattingContext = formatChangeLogAndComments(dbSession, hotspot, users, components, responseBuilder);
      formatUsers(responseBuilder, users, formattingContext);

      writeProtobuf(responseBuilder.build(), request, response);
    }
  }

  private Users loadUsers(DbSession dbSession, IssueDto hotspot) {
    UserDto assignee = ofNullable(hotspot.getAssigneeUuid())
      .map(uuid -> dbClient.userDao().selectByUuid(dbSession, uuid))
      .orElse(null);
    UserDto author = ofNullable(hotspot.getAuthorLogin())
      .map(login -> {
        if (assignee != null && assignee.getLogin().equals(login)) {
          return assignee;
        }
        return dbClient.userDao().selectByLogin(dbSession, login);
      })
      .orElse(null);
    return new Users(assignee, author);
  }

  private static void formatHotspot(ShowWsResponse.Builder builder, IssueDto hotspot, Users users) {
    builder.setKey(hotspot.getKey());
    ofNullable(hotspot.getStatus()).ifPresent(builder::setStatus);
    ofNullable(hotspot.getResolution()).ifPresent(builder::setResolution);
    ofNullable(hotspot.getLine()).ifPresent(builder::setLine);
    ofNullable(emptyToNull(hotspot.getChecksum())).ifPresent(builder::setHash);
    builder.setMessage(nullToEmpty(hotspot.getMessage()));
    builder.addAllMessageFormattings(MessageFormattingUtils.dbMessageFormattingToWs(hotspot.parseMessageFormattings()));
    builder.setCreationDate(formatDateTime(hotspot.getIssueCreationDate()));
    builder.setUpdateDate(formatDateTime(hotspot.getIssueUpdateDate()));
    users.getAssignee().map(UserDto::getLogin).ifPresent(builder::setAssignee);
    Optional.ofNullable(hotspot.getAuthorLogin()).ifPresent(builder::setAuthor);
    builder.addAllCodeVariants(hotspot.getCodeVariants());
  }

  private void formatComponents(Components components, ShowWsResponse.Builder responseBuilder) {
    responseBuilder
      .setProject(responseFormatter.formatProject(Hotspots.Component.newBuilder(), components.getProjectDto(), components.getBranch(), components.getPullRequest()))
      .setComponent(responseFormatter.formatComponent(Hotspots.Component.newBuilder(), components.getComponent(), components.getBranch(), components.getPullRequest()));
    responseBuilder.setCanChangeStatus(hotspotWsSupport.canChangeStatus(components.getProjectDto()));
  }

  private static void formatRule(ShowWsResponse.Builder responseBuilder, RuleDto ruleDto) {
    SecurityStandards securityStandards = SecurityStandards.fromSecurityStandards(ruleDto.getSecurityStandards());
    SecurityStandards.SQCategory sqCategory = securityStandards.getSqCategory();

    Hotspots.Rule.Builder ruleBuilder = Hotspots.Rule.newBuilder()
      .setKey(ruleDto.getKey().toString())
      .setName(nullToEmpty(ruleDto.getName()))
      .setSecurityCategory(sqCategory.getKey())
      .setVulnerabilityProbability(sqCategory.getVulnerability().name());

    Map<String, String> sectionKeyToContent = getSectionKeyToContent(ruleDto);
    Optional.ofNullable(sectionKeyToContent.get(DEFAULT_KEY)).ifPresent(ruleBuilder::setRiskDescription);
    Optional.ofNullable(sectionKeyToContent.get(ROOT_CAUSE_SECTION_KEY)).ifPresent(ruleBuilder::setRiskDescription);
    Optional.ofNullable(sectionKeyToContent.get(ASSESS_THE_PROBLEM_SECTION_KEY)).ifPresent(ruleBuilder::setVulnerabilityDescription);
    Optional.ofNullable(sectionKeyToContent.get(HOW_TO_FIX_SECTION_KEY)).ifPresent(ruleBuilder::setFixRecommendations);
    responseBuilder.setRule(ruleBuilder.build());
  }

  private static Map<String, String> getSectionKeyToContent(RuleDto ruleDefinitionDto) {
    return ruleDefinitionDto.getRuleDescriptionSectionDtos().stream()
      .sorted(comparing(r -> Optional.ofNullable(r.getContext())
        .map(RuleDescriptionSectionContextDto::getKey).orElse("")))
      .collect(toMap(
        RuleDescriptionSectionDto::getKey,
        section -> getContentAndConvertToHtmlIfNecessary(ruleDefinitionDto.getDescriptionFormat(), section),
        (a, b) -> a));
  }

  private static String getContentAndConvertToHtmlIfNecessary(@Nullable RuleDto.Format descriptionFormat, RuleDescriptionSectionDto section) {
    if (RuleDto.Format.MARKDOWN == descriptionFormat) {
      return Markdown.convertToHtml(section.getContent());
    }
    return section.getContent();
  }

  private void formatTextRange(ShowWsResponse.Builder hotspotBuilder, IssueDto hotspot) {
    responseFormatter.formatTextRange(hotspot, hotspotBuilder::setTextRange);
  }

  private void formatFlows(DbSession dbSession, ShowWsResponse.Builder hotspotBuilder, IssueDto hotspot) {
    DbIssues.Locations locations = hotspot.parseLocations();

    if (locations == null) {
      return;
    }

    Set<String> componentUuids = readComponentUuidsFromLocations(hotspot, locations);
    Map<String, ComponentDto> componentsByUuids = loadComponents(dbSession, componentUuids);

    hotspotBuilder.addAllFlows(responseFormatter.formatFlows(locations, hotspotBuilder.getComponent().getKey(), componentsByUuids));
  }

  private static Set<String> readComponentUuidsFromLocations(IssueDto hotspot, Locations locations) {
    Set<String> componentUuids = new HashSet<>();
    componentUuids.add(hotspot.getComponentUuid());
    for (DbIssues.Flow flow : locations.getFlowList()) {
      for (DbIssues.Location location : flow.getLocationList()) {
        if (location.hasComponentId()) {
          componentUuids.add(location.getComponentId());
        }
      }
    }
    return componentUuids;
  }

  private Map<String, ComponentDto> loadComponents(DbSession dbSession, Set<String> componentUuids) {
    Map<String, ComponentDto> componentsByUuids = dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession,
      componentUuids)
      .stream()
      .collect(toMap(ComponentDto::uuid, Function.identity(), (componentDto, componentDto2) -> componentDto2));

    Set<String> componentUuidsToLoad = copyOf(difference(componentUuids, componentsByUuids.keySet()));
    if (!componentUuidsToLoad.isEmpty()) {
      dbClient.componentDao().selectByUuids(dbSession, componentUuidsToLoad)
        .forEach(c -> componentsByUuids.put(c.uuid(), c));
    }
    return componentsByUuids;
  }

  private FormattingContext formatChangeLogAndComments(DbSession dbSession, IssueDto hotspot, Users users, Components components, ShowWsResponse.Builder responseBuilder) {
    Set<UserDto> preloadedUsers = Stream.of(users.getAssignee(), users.getAuthor())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toSet());
    FormattingContext formattingContext = issueChangeSupport
      .newFormattingContext(dbSession, singleton(hotspot), Load.ALL, preloadedUsers, Set.of(components.component));

    issueChangeSupport.formatChangelog(hotspot, formattingContext)
      .forEach(responseBuilder::addChangelog);
    issueChangeSupport.formatComments(hotspot, Common.Comment.newBuilder(), formattingContext)
      .forEach(responseBuilder::addComment);

    return formattingContext;
  }

  private void formatUsers(ShowWsResponse.Builder responseBuilder, Users users, FormattingContext formattingContext) {
    Common.User.Builder userBuilder = Common.User.newBuilder();
    Stream.concat(
      Stream.of(users.getAssignee(), users.getAuthor())
        .filter(Optional::isPresent)
        .map(Optional::get),
      formattingContext.getUsers().stream())
      .distinct()
      .map(user -> userFormatter.formatUser(userBuilder, user))
      .forEach(responseBuilder::addUsers);
  }

  private RuleDto loadRule(DbSession dbSession, IssueDto hotspot) {
    RuleKey ruleKey = hotspot.getRuleKey();
    return dbClient.ruleDao().selectByKey(dbSession, ruleKey)
      .orElseThrow(() -> new NotFoundException(format("Rule '%s' does not exist", ruleKey)));
  }

  private Components loadComponents(DbSession dbSession, IssueDto hotspot) {
    String componentUuid = hotspot.getComponentUuid();
    checkArgument(componentUuid != null, "Hotspot '%s' has no component", hotspot.getKee());

    ProjectAndBranch projectAndBranch = hotspotWsSupport.loadAndCheckBranch(dbSession, hotspot, ProjectPermission.USER);
    BranchDto branch = projectAndBranch.getBranch();
    ComponentDto component = dbClient.componentDao().selectByUuid(dbSession, componentUuid)
      .orElseThrow(() -> new NotFoundException(format("Component with uuid '%s' does not exist", componentUuid)));
    return new Components(projectAndBranch.getProject(), component, branch);
  }

  private static final class Components {
    private final ProjectDto project;
    private final ComponentDto component;
    private final String branch;
    private final String pullRequest;

    private Components(ProjectDto projectDto, ComponentDto component, BranchDto branch) {
      this.project = projectDto;
      this.component = component;
      if (branch.isMain()) {
        this.branch = null;
        this.pullRequest = null;
      } else if (branch.getBranchType() == BranchType.BRANCH) {
        this.branch = branch.getKey();
        this.pullRequest = null;
      } else {
        this.branch = null;
        this.pullRequest = branch.getKey();
      }
    }

    public ProjectDto getProjectDto() {
      return project;
    }

    @CheckForNull
    public String getBranch() {
      return branch;
    }

    @CheckForNull
    public String getPullRequest() {
      return pullRequest;
    }

    public ComponentDto getComponent() {
      return component;
    }
  }

  private static final class Users {
    @CheckForNull
    private final UserDto assignee;
    @CheckForNull
    private final UserDto author;

    private Users(@Nullable UserDto assignee, @Nullable UserDto author) {
      this.assignee = assignee;
      this.author = author;
    }

    public Optional<UserDto> getAssignee() {
      return ofNullable(assignee);
    }

    public Optional<UserDto> getAuthor() {
      return ofNullable(author);
    }
  }
}
