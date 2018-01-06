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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
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
  private List<UserDto> users = null;
  private List<RuleDefinitionDto> rules = null;
  private final Map<String, String> organizationKeysByUuid = new HashMap<>();
  private final Map<String, ComponentDto> componentsByUuid = new HashMap<>();
  private final ListMultimap<String, IssueChangeDto> commentsByIssueKey = ArrayListMultimap.create();
  private final ListMultimap<String, String> actionsByIssueKey = ArrayListMultimap.create();
  private final ListMultimap<String, Transition> transitionsByIssueKey = ArrayListMultimap.create();
  private final Set<String> updatableComments = new HashSet<>();

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
  public ComponentDto getComponentByUuid(String uuid) {
    return componentsByUuid.get(uuid);
  }

  @CheckForNull
  public List<UserDto> getUsers() {
    return users;
  }

  @CheckForNull
  public List<RuleDefinitionDto> getRules() {
    return rules;
  }

  public String getOrganizationKey(String organizationUuid) {
    String organizationKey = organizationKeysByUuid.get(organizationUuid);
    checkNotNull(organizationKey, "Organization for uuid '%s' not found", organizationUuid);
    return organizationKey;
  }

  @CheckForNull
  public List<IssueChangeDto> getCommentsForIssueKey(String issueKey) {
    if (commentsByIssueKey.containsKey(issueKey)) {
      return commentsByIssueKey.get(issueKey);
    }
    return null;
  }

  @CheckForNull
  public List<String> getActionsForIssueKey(String issueKey) {
    if (actionsByIssueKey.containsKey(issueKey)) {
      return actionsByIssueKey.get(issueKey);
    }
    return null;
  }

  @CheckForNull
  public List<Transition> getTransitionsForIssueKey(String issueKey) {
    if (transitionsByIssueKey.containsKey(issueKey)) {
      return transitionsByIssueKey.get(issueKey);
    }
    return null;
  }

  public void setUsers(@Nullable List<UserDto> users) {
    this.users = users;
  }

  public void setRules(@Nullable List<RuleDefinitionDto> rules) {
    this.rules = rules;
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

  public void addActions(String issueKey, List<String> actions) {
    actionsByIssueKey.putAll(issueKey, actions);
  }

  public void addTransitions(String issueKey, List<Transition> transitions) {
    transitionsByIssueKey.putAll(issueKey, transitions);
  }

  public void addUpdatableComment(String commentKey) {
    updatableComments.add(commentKey);
  }

  public boolean isUpdatableComment(String commentKey) {
    return updatableComments.contains(commentKey);
  }

  @CheckForNull
  public Long getEffortTotal() {
    return effortTotal;
  }

  public void setEffortTotal(@Nullable Long effortTotal) {
    this.effortTotal = effortTotal;
  }

  public void addOrganization(OrganizationDto organizationDto) {
    this.organizationKeysByUuid.put(organizationDto.getUuid(), organizationDto.getKey());
  }
}
