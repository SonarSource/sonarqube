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
package org.sonar.ce.task.projectanalysis.notification;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Durations;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification.DetailsSupplier;
import org.sonar.server.issue.notification.NewIssuesNotification.RuleDefinition;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.FILE;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

@ComputeEngineSide
public class NotificationFactory {
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final RuleRepository ruleRepository;
  private final Durations durations;
  private final IssuesChangesNotificationSerializer issuesChangesSerializer;
  private Map<String, Component> componentsByUuid;

  public NotificationFactory(TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    RuleRepository ruleRepository, Durations durations, IssuesChangesNotificationSerializer issuesChangesSerializer) {
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.ruleRepository = ruleRepository;
    this.durations = durations;
    this.issuesChangesSerializer = issuesChangesSerializer;
  }

  public MyNewIssuesNotification newMyNewIssuesNotification(Map<String, UserDto> assigneesByUuid) {
    verifyAssigneesByUuid(assigneesByUuid);
    return new MyNewIssuesNotification(durations, new DetailsSupplierImpl(assigneesByUuid));
  }

  public NewIssuesNotification newNewIssuesNotification(Map<String, UserDto> assigneesByUuid) {
    verifyAssigneesByUuid(assigneesByUuid);
    return new NewIssuesNotification(durations, new DetailsSupplierImpl(assigneesByUuid));
  }

  public IssuesChangesNotification newIssuesChangesNotification(Set<DefaultIssue> issues, Map<String, UserDto> assigneesByUuid) {
    AnalysisChange change = new AnalysisChange(analysisMetadataHolder.getAnalysisDate());
    Set<ChangedIssue> changedIssues = issues.stream()
      .map(issue -> new ChangedIssue.Builder(issue.key())
        .setAssignee(getAssignee(issue.assignee(), assigneesByUuid))
        .setNewResolution(issue.resolution())
        .setNewStatus(issue.status())
        .setRule(getRuleByRuleKey(issue.ruleKey()))
        .setProject(getProject())
        .build())
      .collect(MoreCollectors.toSet(issues.size()));

    return issuesChangesSerializer.serialize(new IssuesChangesNotificationBuilder(changedIssues, change));
  }

  @CheckForNull
  public User getAssignee(@Nullable String assigneeUuid, Map<String, UserDto> assigneesByUuid) {
    if (assigneeUuid == null) {
      return null;
    }
    UserDto dto = assigneesByUuid.get(assigneeUuid);
    checkState(dto != null, "Can not find DTO for assignee uuid %s", assigneeUuid);
    return new User(dto.getUuid(), dto.getLogin(), dto.getName());
  }

  private IssuesChangesNotificationBuilder.Rule getRuleByRuleKey(RuleKey ruleKey) {
    return ruleRepository.findByKey(ruleKey)
      .map(t -> new IssuesChangesNotificationBuilder.Rule(ruleKey, t.getName()))
      .orElseThrow(() -> new IllegalStateException("Can not find rule " + ruleKey + " in RuleRepository"));
  }

  private Project getProject() {
    Component project = treeRootHolder.getRoot();
    Branch branch = analysisMetadataHolder.getBranch();
    Project.Builder builder = new Project.Builder(project.getUuid())
      .setKey(project.getKey())
      .setProjectName(project.getName());
    if (branch.getType() != PULL_REQUEST && !branch.isMain()) {
      builder.setBranchName(branch.getName());
    }
    return builder.build();
  }

  private static void verifyAssigneesByUuid(Map<String, UserDto> assigneesByUuid) {
    requireNonNull(assigneesByUuid, "assigneesByUuid can't be null");
  }

  private class DetailsSupplierImpl implements DetailsSupplier {
    private final Map<String, UserDto> assigneesByUuid;

    private DetailsSupplierImpl(Map<String, UserDto> assigneesByUuid) {
      this.assigneesByUuid = assigneesByUuid;
    }

    @Override
    public Optional<RuleDefinition> getRuleDefinitionByRuleKey(RuleKey ruleKey) {
      requireNonNull(ruleKey, "ruleKey can't be null");
      return ruleRepository.findByKey(ruleKey)
        .map(t -> new RuleDefinition(t.getName(), t.getLanguage()));
    }

    @Override
    public Optional<String> getComponentNameByUuid(String uuid) {
      requireNonNull(uuid, "uuid can't be null");
      return Optional.ofNullable(lazyLoadComponentsByUuid().get(uuid))
        .map(t -> t.getType() == Component.Type.FILE || t.getType() == Component.Type.DIRECTORY ? t.getShortName() : t.getName());
    }

    private Map<String, Component> lazyLoadComponentsByUuid() {
      if (componentsByUuid == null) {
        ImmutableMap.Builder<String, Component> builder = ImmutableMap.builder();
        new DepthTraversalTypeAwareCrawler(new TypeAwareVisitorAdapter(FILE, PRE_ORDER) {
          @Override
          public void visitAny(Component any) {
            builder.put(any.getUuid(), any);
          }
        }).visit(treeRootHolder.getRoot());
        componentsByUuid = builder.build();
      }
      return componentsByUuid;
    }

    @Override
    public Optional<String> getUserNameByUuid(String uuid) {
      requireNonNull(uuid, "uuid can't be null");
      return Optional.ofNullable(assigneesByUuid.get(uuid))
        .map(UserDto::getName);
    }
  }
}
