/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueChangeWSSupport;
import org.sonar.server.issue.IssueChangeWSSupport.FormattingContext;
import org.sonar.server.issue.IssueChangeWSSupport.Load;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.ws.UserResponseFormatter;
import org.sonar.server.rule.HotspotRuleDescription;
import org.sonar.server.security.SecurityStandards;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.ShowWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ShowAction implements HotspotsWsAction {

  private static final String PARAM_HOTSPOT_KEY = "hotspot";

  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;
  private final HotspotWsResponseFormatter responseFormatter;
  private final TextRangeResponseFormatter textRangeFormatter;
  private final UserResponseFormatter userFormatter;
  private final IssueChangeWSSupport issueChangeSupport;

  public ShowAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport,
    HotspotWsResponseFormatter responseFormatter, TextRangeResponseFormatter textRangeFormatter,
    UserResponseFormatter userFormatter, IssueChangeWSSupport issueChangeSupport) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.responseFormatter = responseFormatter;
    this.textRangeFormatter = textRangeFormatter;
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
      .setInternal(true);

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
      RuleDefinitionDto rule = loadRule(dbSession, hotspot);

      ShowWsResponse.Builder responseBuilder = ShowWsResponse.newBuilder();
      formatHotspot(responseBuilder, hotspot, users);
      formatComponents(components, responseBuilder);
      formatRule(responseBuilder, rule);
      formatTextRange(responseBuilder, hotspot);
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
    builder.setMessage(nullToEmpty(hotspot.getMessage()));
    builder.setCreationDate(formatDateTime(hotspot.getIssueCreationDate()));
    builder.setUpdateDate(formatDateTime(hotspot.getIssueUpdateDate()));
    users.getAssignee().map(UserDto::getLogin).ifPresent(builder::setAssignee);
    Optional.ofNullable(hotspot.getAuthorLogin()).ifPresent(builder::setAuthor);
  }

  private void formatComponents(Components components, ShowWsResponse.Builder responseBuilder) {
    responseBuilder
      .setProject(responseFormatter.formatComponent(Hotspots.Component.newBuilder(), components.getProject()))
      .setComponent(responseFormatter.formatComponent(Hotspots.Component.newBuilder(), components.getComponent()));
    responseBuilder.setCanChangeStatus(hotspotWsSupport.canChangeStatus(components.getProject()));
  }

  private static void formatRule(ShowWsResponse.Builder responseBuilder, RuleDefinitionDto ruleDefinitionDto) {
    SecurityStandards securityStandards = SecurityStandards.fromSecurityStandards(ruleDefinitionDto.getSecurityStandards());
    SecurityStandards.SQCategory sqCategory = securityStandards.getSqCategory();
    HotspotRuleDescription hotspotRuleDescription = HotspotRuleDescription.from(ruleDefinitionDto);
    Hotspots.Rule.Builder ruleBuilder = Hotspots.Rule.newBuilder()
      .setKey(ruleDefinitionDto.getKey().toString())
      .setName(nullToEmpty(ruleDefinitionDto.getName()))
      .setSecurityCategory(sqCategory.getKey())
      .setVulnerabilityProbability(sqCategory.getVulnerability().name());
    hotspotRuleDescription.getVulnerable().ifPresent(ruleBuilder::setVulnerabilityDescription);
    hotspotRuleDescription.getRisk().ifPresent(ruleBuilder::setRiskDescription);
    hotspotRuleDescription.getFixIt().ifPresent(ruleBuilder::setFixRecommendations);
    responseBuilder.setRule(ruleBuilder.build());
  }

  private void formatTextRange(ShowWsResponse.Builder responseBuilder, IssueDto hotspot) {
    textRangeFormatter.formatTextRange(hotspot, responseBuilder::setTextRange);
  }

  private FormattingContext formatChangeLogAndComments(DbSession dbSession, IssueDto hotspot, Users users, Components components, ShowWsResponse.Builder responseBuilder) {
    Set<UserDto> preloadedUsers = Stream.of(users.getAssignee(), users.getAuthor())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toSet());
    Set<ComponentDto> preloadedComponents = ImmutableSet.of(components.project, components.component);
    FormattingContext formattingContext = issueChangeSupport
      .newFormattingContext(dbSession, singleton(hotspot), Load.ALL, preloadedUsers, preloadedComponents);

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

  private RuleDefinitionDto loadRule(DbSession dbSession, IssueDto hotspot) {
    RuleKey ruleKey = hotspot.getRuleKey();
    return dbClient.ruleDao().selectDefinitionByKey(dbSession, ruleKey)
      .orElseThrow(() -> new NotFoundException(format("Rule '%s' does not exist", ruleKey)));
  }

  private Components loadComponents(DbSession dbSession, IssueDto hotspot) {
    String componentUuid = hotspot.getComponentUuid();

    ComponentDto project = hotspotWsSupport.loadAndCheckProject(dbSession, hotspot, UserRole.USER);

    checkArgument(componentUuid != null, "Hotspot '%s' has no component", hotspot.getKee());
    boolean hotspotOnProject = Objects.equals(project.uuid(), componentUuid);
    ComponentDto component = hotspotOnProject ? project
      : dbClient.componentDao().selectByUuid(dbSession, componentUuid)
        .orElseThrow(() -> new NotFoundException(format("Component with uuid '%s' does not exist", componentUuid)));

    return new Components(project, component);
  }

  private static final class Components {
    private final ComponentDto project;
    private final ComponentDto component;

    private Components(ComponentDto project, ComponentDto component) {
      this.project = project;
      this.component = component;
    }

    public ComponentDto getProject() {
      return project;
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
