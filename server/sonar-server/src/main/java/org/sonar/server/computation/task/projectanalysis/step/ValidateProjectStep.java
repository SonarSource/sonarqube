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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.sonar.api.utils.DateUtils.formatDateTime;

/**
 * Validate project and modules. It will fail in the following cases :
 * <ol>
 * <li>module key is not valid</li>
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

  public ValidateProjectStep(DbClient dbClient, BatchReportReader reportReader, TreeRootHolder treeRootHolder,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Component root = treeRootHolder.getRoot();
      List<ComponentDto> baseModules = dbClient.componentDao().selectEnabledModulesFromProjectKey(dbSession, root.getKey());
      Map<String, ComponentDto> baseModulesByKey = from(baseModules).uniqueIndex(ComponentDto::getDbKey);
      ValidateProjectsVisitor visitor = new ValidateProjectsVisitor(dbSession, dbClient.componentDao(), baseModulesByKey);
      new DepthTraversalTypeAwareCrawler(visitor).visit(root);

      if (!visitor.validationMessages.isEmpty()) {
        throw MessageException.of("Validation of project failed:\n  o " + MESSAGES_JOINER.join(visitor.validationMessages));
      }
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
      String rawProjectKey = rawProject.getKey();

      Optional<ComponentDto> baseProject = loadBaseComponent(rawProjectKey);
      validateAnalysisDate(baseProject);
    }

    private void validateAnalysisDate(Optional<ComponentDto> baseProject) {
      if (baseProject.isPresent()) {
        java.util.Optional<SnapshotDto> snapshotDto = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(session, baseProject.get().uuid());
        long currentAnalysisDate = analysisMetadataHolder.getAnalysisDate();
        Long lastAnalysisDate = snapshotDto.map(SnapshotDto::getCreatedAt).orElse(null);
        if (lastAnalysisDate != null && currentAnalysisDate <= lastAnalysisDate) {
          validationMessages.add(format("Date of analysis cannot be older than the date of the last known analysis on this project. Value: \"%s\". " +
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
        validationMessages.add(format("The project \"%s\" is already defined in SonarQube but not as a module of project \"%s\". "
          + "If you really want to stop directly analysing project \"%s\", please first delete it from SonarQube and then relaunch the analysis of project \"%s\".",
          rawModuleKey, rawProjectKey, rawModuleKey, rawProjectKey));
      }
    }

    private void validateModuleKeyIsNotAlreadyUsedInAnotherProject(ComponentDto baseModule, String rawModuleKey) {
      if (!baseModule.projectUuid().equals(baseModule.uuid()) && !baseModule.projectUuid().equals(rawProject.getUuid())) {
        ComponentDto projectModule = componentDao.selectOrFailByUuid(session, baseModule.projectUuid());
        validationMessages.add(format("Module \"%s\" is already part of project \"%s\"", rawModuleKey, projectModule.getDbKey()));
      }
    }

    private void validateBatchKey(Component rawComponent) {
      String batchKey = reportReader.readComponent(rawComponent.getReportAttributes().getRef()).getKey();
      if (!ComponentKeys.isValidModuleKey(batchKey)) {
        validationMessages.add(format("\"%s\" is not a valid project or module key. "
          + "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", batchKey));
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
