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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.workflow.Transition;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * All the data required to write response of api/issues/search
 */
public class SearchResponseData {

  private final List<IssueDto> issues;

  private Long effortTotal = null;
  private final Map<String, UserDto> usersByUuid = new HashMap<>();
  private final List<RuleDefinitionDto> rules = new ArrayList<>();
  private final Map<String, String> organizationKeysByUuid = new HashMap<>();
  private final Map<String, ComponentDto> componentsByUuid = new HashMap<>();
  private final ListMultimap<String, IssueChangeDto> commentsByIssueKey = ArrayListMultimap.create();
  private final ListMultimap<String, String> actionsByIssueKey = ArrayListMultimap.create();
  private final ListMultimap<String, Transition> transitionsByIssueKey = ArrayListMultimap.create();
  private final Set<String> updatableComments = new HashSet<>();
  private final Set<String> userOrganizationUuids = new HashSet<>();

  public SearchResponseData(IssueDto issue) {
    checkNotNull(issue);
    this.issues = ImmutableList.of(issue);
  }

  public SearchResponseData(List<IssueDto> issues) {
    checkNotNull(issues);
    this.issues = issues;
  }

  public List<IssueDto> getIssues() {
    return issues;
  }

  public Collection<ComponentDto> getComponents() {
    return componentsByUuid.values();
  }

  @CheckForNull
  ComponentDto getComponentByUuid(String uuid) {
    return componentsByUuid.get(uuid);
  }

  public List<UserDto> getUsers() {
    return new ArrayList<>(usersByUuid.values());
  }

  public List<RuleDefinitionDto> getRules() {
    return rules;
  }

  public String getOrganizationKey(String organizationUuid) {
    String organizationKey = organizationKeysByUuid.get(organizationUuid);
    checkNotNull(organizationKey, "Organization for uuid '%s' not found", organizationUuid);
    return organizationKey;
  }

  @CheckForNull
  List<IssueChangeDto> getCommentsForIssueKey(String issueKey) {
    if (commentsByIssueKey.containsKey(issueKey)) {
      return commentsByIssueKey.get(issueKey);
    }
    return null;
  }

  @CheckForNull
  List<String> getActionsForIssueKey(String issueKey) {
    if (actionsByIssueKey.containsKey(issueKey)) {
      return actionsByIssueKey.get(issueKey);
    }
    return null;
  }

  @CheckForNull
  List<Transition> getTransitionsForIssueKey(String issueKey) {
    if (transitionsByIssueKey.containsKey(issueKey)) {
      return transitionsByIssueKey.get(issueKey);
    }
    return null;
  }

  void addUsers(@Nullable List<UserDto> users) {
    if (users != null) {
      users.forEach(u -> usersByUuid.put(u.getUuid(), u));
    }
  }

  public void addRules(@Nullable List<RuleDefinitionDto> rules) {
    if (rules != null) {
      this.rules.addAll(rules);
    }
  }

  public void setComments(@Nullable List<IssueChangeDto> comments) {
    for (IssueChangeDto comment : comments) {
      commentsByIssueKey.put(comment.getIssueKey(), comment);
    }
  }

  public void addComponents(@Nullable Collection<ComponentDto> dtos) {
    if (dtos != null) {
      for (ComponentDto dto : dtos) {
        componentsByUuid.put(dto.uuid(), dto);
      }
    }
  }

  void addActions(String issueKey, Iterable<String> actions) {
    actionsByIssueKey.putAll(issueKey, actions);
  }

  void addTransitions(String issueKey, List<Transition> transitions) {
    transitionsByIssueKey.putAll(issueKey, transitions);
  }

  void addUpdatableComment(String commentKey) {
    updatableComments.add(commentKey);
  }

  boolean isUpdatableComment(String commentKey) {
    return updatableComments.contains(commentKey);
  }

  @CheckForNull
  Long getEffortTotal() {
    return effortTotal;
  }

  void setEffortTotal(@Nullable Long effortTotal) {
    this.effortTotal = effortTotal;
  }

  void addOrganization(OrganizationDto organizationDto) {
    this.organizationKeysByUuid.put(organizationDto.getUuid(), organizationDto.getKey());
  }

  void setUserOrganizationUuids(Set<String> organizationUuids) {
    this.userOrganizationUuids.addAll(organizationUuids);
  }

  Set<String> getUserOrganizationUuids() {
    return this.userOrganizationUuids;
  }

  @CheckForNull
  UserDto getUserByUuid(@Nullable String userUuid) {
    UserDto userDto = usersByUuid.get(userUuid);
    if (userDto == null) {
      return null;
    }
    return userDto;
  }
}
