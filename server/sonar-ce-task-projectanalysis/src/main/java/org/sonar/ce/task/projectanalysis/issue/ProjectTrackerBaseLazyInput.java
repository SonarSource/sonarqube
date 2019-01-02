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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportModulesPath;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.apache.commons.lang.StringUtils.trimToEmpty;

class ProjectTrackerBaseLazyInput extends BaseInputFactory.BaseLazyInput {

  private AnalysisMetadataHolder analysisMetadataHolder;
  private ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues;
  private DbClient dbClient;
  private IssueFieldsSetter issueUpdater;
  private ComponentIssuesLoader issuesLoader;
  private ReportModulesPath reportModulesPath;

  ProjectTrackerBaseLazyInput(AnalysisMetadataHolder analysisMetadataHolder, ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues, DbClient dbClient,
    IssueFieldsSetter issueUpdater, ComponentIssuesLoader issuesLoader, ReportModulesPath reportModulesPath, Component component) {
    super(dbClient, component, null);
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.componentsWithUnprocessedIssues = componentsWithUnprocessedIssues;
    this.dbClient = dbClient;
    this.issueUpdater = issueUpdater;
    this.issuesLoader = issuesLoader;
    this.reportModulesPath = reportModulesPath;
  }

  @Override
  protected List<DefaultIssue> loadIssues() {
    List<DefaultIssue> result = new ArrayList<>();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> dirOrModulesUuidsWithIssues = dbClient.issueDao().selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(dbSession, component.getUuid());
      if (!dirOrModulesUuidsWithIssues.isEmpty()) {
        Map<String, String> pathByModuleKey = reportModulesPath.get();
        // Migrate issues that were previously on modules or directories to the root project
        Map<String, ComponentDto> modulesByUuid = dbClient.componentDao().selectProjectAndModulesFromProjectKey(dbSession, component.getDbKey(), true)
          .stream().collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
        List<ComponentDto> dirOrModulesWithIssues = dbClient.componentDao().selectByUuids(dbSession, dirOrModulesUuidsWithIssues);
        dirOrModulesWithIssues.forEach(c -> {
          List<DefaultIssue> issuesOnModuleOrDir = issuesLoader.loadOpenIssues(c.uuid());
          String moduleOrDirProjectRelativePath = c.qualifier().equals(Qualifiers.MODULE) ? buildModuleProjectRelativePath(pathByModuleKey, c)
            : buildDirectoryProjectRelativePath(pathByModuleKey, c, modulesByUuid.get(c.moduleUuid()));
          result.addAll(migrateIssuesToTheRoot(issuesOnModuleOrDir, moduleOrDirProjectRelativePath));
          componentsWithUnprocessedIssues.remove(c.uuid());
        });
      }
      result.addAll(issuesLoader.loadOpenIssues(effectiveUuid));
      return result;
    }
  }

  private static String buildDirectoryProjectRelativePath(Map<String, String> pathByModuleKey, ComponentDto c, ComponentDto parentModule) {
    String moduleProjectRelativePath = buildModuleProjectRelativePath(pathByModuleKey, parentModule);
    return Stream.of(moduleProjectRelativePath, c.path())
      .map(StringUtils::trimToNull)
      .filter(s -> s != null && !"/".equals(s))
      .collect(Collectors.joining("/"));
  }

  private static String buildModuleProjectRelativePath(Map<String, String> pathByModuleKey, ComponentDto parentModule) {
    return Optional.ofNullable(pathByModuleKey.get(parentModule.getKey()))
      // If module is not in the scanner report, we can't guess the path accurately. Fallback on what we have in DB
      .orElse(trimToEmpty(parentModule.path()));
  }

  private Collection<? extends DefaultIssue> migrateIssuesToTheRoot(List<DefaultIssue> issuesOnModule, String modulePath) {
    for (DefaultIssue i : issuesOnModule) {
      // changes the issue's component uuid, add a change and set issue as changed to enforce it is persisted to DB
      IssueChangeContext context = IssueChangeContext.createUser(new Date(analysisMetadataHolder.getAnalysisDate()), null);
      if (StringUtils.isNotBlank(modulePath)) {
        issueUpdater.setMessage(i, "[" + modulePath + "] " + i.getMessage(), context);
      }
      issueUpdater.setIssueMoved(i, component.getUuid(), context);
      // other fields (such as module, modulePath, componentKey) are read-only and set/reset for consistency only
      i.setComponentKey(component.getKey());
      i.setModuleUuid(null);
      i.setModuleUuidPath(null);
    }
    return issuesOnModule;
  }
}
