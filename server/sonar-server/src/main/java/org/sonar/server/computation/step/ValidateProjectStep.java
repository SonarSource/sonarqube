/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.component.ComponentDtoFunctions.toKey;

/**
 * Validate project and modules. It will fail in the following cases :
 * <ol>
 * <li>branch is not valid</li>
 * <li>project or module key is not valid</li>
 * <li>module key already exists in another project (same module key cannot exists in different projects)</li>
 * <li>module key is already used as a project key</li>
 * <li>date of the analysis is before last analysis</li>
 * </ol>
 */
public class ValidateProjectStep implements ComputationStep {

  private static final Joiner MESSAGES_JOINER = Joiner.on("\n  o ");

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public ValidateProjectStep(DbClient dbClient, BatchReportReader reportReader, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      List<ComponentDto> baseModules = dbClient.componentDao().selectEnabledModulesFromProjectKey(session, treeRootHolder.getRoot().getKey());
      Map<String, ComponentDto> baseModulesByKey = FluentIterable.from(baseModules).uniqueIndex(toKey());
      ValidateProjectsVisitor visitor = new ValidateProjectsVisitor(session, dbClient.componentDao(),
        baseModulesByKey);
      new DepthTraversalTypeAwareCrawler(visitor).visit(treeRootHolder.getRoot());

      if (!visitor.validationMessages.isEmpty()) {
        throw MessageException.of("Validation of project failed:\n  o " + MESSAGES_JOINER.join(visitor.validationMessages));
      }
    } finally {
      dbClient.closeSession(session);
    }
  }

  @Override
  public String getDescription() {
    return "Validate project";
  }

  private class ValidateProjectsVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;
    private final ComponentDao componentDao;
    private final Map<String, ComponentDto> baseModulesByKey;
    private final List<String> validationMessages = new ArrayList<>();

    private Component rawProject;

    public ValidateProjectsVisitor(DbSession session, ComponentDao componentDao, Map<String, ComponentDto> baseModulesByKey) {
      super(CrawlerDepthLimit.MODULE, ComponentVisitor.Order.PRE_ORDER);
      this.session = session;
      this.componentDao = componentDao;

      this.baseModulesByKey = baseModulesByKey;
    }

    @Override
    public void visitProject(Component rawProject) {
      this.rawProject = rawProject;
      validateBranch();
      validateBatchKey(rawProject);

      String rawProjectKey = rawProject.getKey();
      Optional<ComponentDto> baseProject = loadBaseComponent(rawProjectKey);
      validateProjectKey(baseProject, rawProjectKey);
      validateAnalysisDate(baseProject);
    }

    private void validateProjectKey(Optional<ComponentDto> baseProject, String rawProjectKey) {
      if (baseProject.isPresent() && !baseProject.get().projectUuid().equals(baseProject.get().uuid())) {
        // Project key is already used as a module of another project
        ComponentDto anotherBaseProject = componentDao.selectOrFailByUuid(session, baseProject.get().projectUuid());
        validationMessages.add(String.format("The project \"%s\" is already defined in SonarQube but as a module of project \"%s\". "
          + "If you really want to stop directly analysing project \"%s\", please first delete it from SonarQube and then relaunch the analysis of project \"%s\".",
          rawProjectKey, anotherBaseProject.key(), anotherBaseProject.key(), rawProjectKey));
      }
    }

    private void validateAnalysisDate(Optional<ComponentDto> baseProject) {
      if (baseProject.isPresent()) {
        SnapshotDto snapshotDto = dbClient.snapshotDao().selectLastSnapshotByComponentId(session, baseProject.get().getId());
        long currentAnalysisDate = analysisMetadataHolder.getAnalysisDate();
        Long lastAnalysisDate = snapshotDto != null ? snapshotDto.getCreatedAt() : null;
        if (lastAnalysisDate != null && currentAnalysisDate <= snapshotDto.getCreatedAt()) {
          validationMessages.add(String.format("Date of analysis cannot be older than the date of the last known analysis on this project. Value: \"%s\". " +
            "Latest analysis: \"%s\". It's only possible to rebuild the past in a chronological order.",
            formatDateTime(new Date(currentAnalysisDate)), formatDateTime(new Date(lastAnalysisDate))));
        }
      }
    }

    @Override
    public void visitModule(Component rawModule) {
      String rawProjectKey = rawProject.getKey();
      String rawModuleKey = rawModule.getKey();
      validateBatchKey(rawModule);

      Optional<ComponentDto> baseModule = loadBaseComponent(rawModuleKey);
      if (!baseModule.isPresent()) {
        return;
      }
      validateModuleIsNotAlreadyUsedAsProject(baseModule.get(), rawProjectKey, rawModuleKey);
      validateModuleKeyIsNotAlreadyUsedInAnotherProject(baseModule.get(), rawModuleKey);
    }

    private void validateModuleIsNotAlreadyUsedAsProject(ComponentDto baseModule, String rawProjectKey, String rawModuleKey) {
      if (baseModule.projectUuid().equals(baseModule.uuid())) {
        // module is actually a project
        validationMessages.add(String.format("The project \"%s\" is already defined in SonarQube but not as a module of project \"%s\". "
          + "If you really want to stop directly analysing project \"%s\", please first delete it from SonarQube and then relaunch the analysis of project \"%s\".",
          rawModuleKey, rawProjectKey, rawModuleKey, rawProjectKey));
      }
    }

    private void validateModuleKeyIsNotAlreadyUsedInAnotherProject(ComponentDto baseModule, String rawModuleKey) {
      if (!baseModule.projectUuid().equals(baseModule.uuid()) && !baseModule.projectUuid().equals(rawProject.getUuid())) {
        ComponentDto projectModule = componentDao.selectOrFailByUuid(session, baseModule.projectUuid());
        validationMessages.add(String.format("Module \"%s\" is already part of project \"%s\"", rawModuleKey, projectModule.key()));
      }
    }

    private void validateBatchKey(Component rawComponent) {
      String batchKey = reportReader.readComponent(rawComponent.getReportAttributes().getRef()).getKey();
      if (!ComponentKeys.isValidModuleKey(batchKey)) {
        validationMessages.add(String.format("\"%s\" is not a valid project or module key. "
          + "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", batchKey));
      }
    }

    @CheckForNull
    private void validateBranch() {
      String branch = analysisMetadataHolder.getBranch();
      if (branch == null) {
        return;
      }
      if (!ComponentKeys.isValidBranch(branch)) {
        validationMessages.add(String.format("\"%s\" is not a valid branch name. "
          + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", branch));
      }
    }

    private Optional<ComponentDto> loadBaseComponent(String rawComponentKey) {
      ComponentDto baseComponent = baseModulesByKey.get(rawComponentKey);
      if (baseComponent == null) {
        // Load component from key to be able to detect issue (try to analyze a module, etc.)
        return componentDao.selectByKey(session, rawComponentKey);
      }
      return Optional.of(baseComponent);
    }
  }

}
