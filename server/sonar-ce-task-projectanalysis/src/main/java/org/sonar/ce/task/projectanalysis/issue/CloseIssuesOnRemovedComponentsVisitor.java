/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.List;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.pushevent.PushEvent;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventRepository;
import org.sonar.ce.task.projectanalysis.pushevent.TaintVulnerabilityClosed;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache.CacheAppender;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.issue.TaintChecker;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Close issues on removed components
 */
public class CloseIssuesOnRemovedComponentsVisitor extends TypeAwareVisitorAdapter {

  private final ComponentIssuesLoader issuesLoader;
  private final ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues;
  private final ProtoIssueCache protoIssueCache;
  private final IssueLifecycle issueLifecycle;
  private final PushEventRepository pushEventRepository;
  private final TaintChecker taintChecker;


  public CloseIssuesOnRemovedComponentsVisitor(ComponentIssuesLoader issuesLoader, ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues, ProtoIssueCache protoIssueCache,
    IssueLifecycle issueLifecycle, PushEventRepository pushEventRepository, TaintChecker taintChecker) {
    super(CrawlerDepthLimit.PROJECT, POST_ORDER);
    this.issuesLoader = issuesLoader;
    this.componentsWithUnprocessedIssues = componentsWithUnprocessedIssues;
    this.protoIssueCache = protoIssueCache;
    this.issueLifecycle = issueLifecycle;
    this.pushEventRepository = pushEventRepository;
    this.taintChecker = taintChecker;
  }

  @Override
  public void visitProject(Component project) {
    closeIssuesForDeletedComponentUuids(componentsWithUnprocessedIssues.getUuids());
  }

  private void closeIssuesForDeletedComponentUuids(Set<String> deletedComponentUuids) {
    try (CacheAppender<DefaultIssue> cacheAppender = protoIssueCache.newAppender()) {
      for (String deletedComponentUuid : deletedComponentUuids) {
        List<DefaultIssue> issues = issuesLoader.loadOpenIssues(deletedComponentUuid);
        for (DefaultIssue issue : issues) {
          issue.setBeingClosed(true);
          // TODO should be renamed
          issue.setOnDisabledRule(false);
          issueLifecycle.doAutomaticTransition(issue);
          cacheAppender.append(issue);
          addPushEventIfTaintVulnerability(issue);
        }
      }
    }
  }

  private void addPushEventIfTaintVulnerability(DefaultIssue issue) {
    if (taintChecker.isTaintVulnerability(issue)) {
      TaintVulnerabilityClosed event = new TaintVulnerabilityClosed(issue.key(), issue.projectKey());
      PushEvent<?> pushEvent = new PushEvent<TaintVulnerabilityClosed>().setName("TaintVulnerabilityClosed").setData(event);
      pushEventRepository.add(pushEvent);
    }
  }

}
