/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.db.DbClient;

/**
 * Validate project and modules. It will fail in the following cases :
 * <ol>
 * <li>property {@link org.sonar.api.CoreProperties#CORE_PREVENT_AUTOMATIC_PROJECT_CREATION} is set to true and project does not exists</li>
 * <li>branch is not valid</li>
 * <li>project or module key is not valid</li>
 * <li>module key already exists in another project (same module key cannot exists in different projects)</li>
 * <li>module key is already used as a project key</li>
 * </ol>
 */
public class ValidateProjectStep implements ComputationStep {

  private static final Joiner MESSAGES_JOINER = Joiner.on("\n  o ");

  private final DbClient dbClient;
  private final Settings settings;
  private final BatchReportReader reportReader;
  private final TreeRootHolder treeRootHolder;

  public ValidateProjectStep(DbClient dbClient, Settings settings, BatchReportReader reportReader, TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.settings = settings;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      List<ComponentDto> modules = dbClient.componentDao().selectModulesFromProjectKey(session, treeRootHolder.getRoot().getKey());
      Map<String, ComponentDto> modulesByKey = Maps.uniqueIndex(modules, new Function<ComponentDto, String>() {
        @Override
        public String apply(@Nonnull ComponentDto input) {
          return input.key();
        }
      });
      ValidateProjectsVisitor visitor = new ValidateProjectsVisitor(session, dbClient.componentDao(),
        settings.getBoolean(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION), modulesByKey);
      visitor.visit(treeRootHolder.getRoot());

      if (!visitor.validationMessages.isEmpty()) {
        throw MessageException.of("Validation of project failed:\n  o " + MESSAGES_JOINER.join(visitor.validationMessages));
      }
    } finally {
      session.close();
    }
  }

  @Override
  public String getDescription() {
    return "Validate project and modules keys";
  }

  private class ValidateProjectsVisitor extends DepthTraversalTypeAwareVisitor {
    private final DbSession session;
    private final ComponentDao componentDao;
    private final boolean preventAutomaticProjectCreation;
    private final Map<String, ComponentDto> modulesByKey;
    private final List<String> validationMessages = new ArrayList<>();

    private Component root;

    public ValidateProjectsVisitor(DbSession session, ComponentDao componentDao, boolean preventAutomaticProjectCreation, Map<String, ComponentDto> modulesByKey) {
      super(Component.Type.MODULE, Order.PRE_ORDER);
      this.session = session;
      this.componentDao = componentDao;

      this.preventAutomaticProjectCreation = preventAutomaticProjectCreation;
      this.modulesByKey = modulesByKey;
    }

    @Override
    public void visitProject(Component project) {
      this.root = project;
      validateBranch();
      validateBatchKey(project);

      String projectKey = project.getKey();
      ComponentDto projectDto = loadComponent(projectKey);
      if (projectDto == null) {
        if (preventAutomaticProjectCreation) {
          validationMessages.add(String.format("Unable to scan non-existing project '%s'", projectKey));
        }
      } else if (!projectDto.projectUuid().equals(projectDto.uuid())) {
        // Project key is already used as a module of another project
        ComponentDto anotherProject = componentDao.selectByUuid(session, projectDto.projectUuid());
        validationMessages.add(String.format("The project \"%s\" is already defined in SonarQube but as a module of project \"%s\". "
          + "If you really want to stop directly analysing project \"%s\", please first delete it from SonarQube and then relaunch the analysis of project \"%s\".",
          projectKey, anotherProject.key(), anotherProject.key(), projectKey));
      }
    }

    @Override
    public void visitModule(Component module) {
      String projectKey = root.getKey();
      String moduleKey = module.getKey();
      validateBatchKey(module);

      ComponentDto moduleDto = loadComponent(moduleKey);
      if (moduleDto == null) {
        return;
      }
      if (moduleDto.projectUuid().equals(moduleDto.uuid())) {
        // module is actually a project
        validationMessages.add(String.format("The project \"%s\" is already defined in SonarQube but not as a module of project \"%s\". "
          + "If you really want to stop directly analysing project \"%s\", please first delete it from SonarQube and then relaunch the analysis of project \"%s\".",
          moduleKey, projectKey, moduleKey, projectKey));
      } else if (!moduleDto.projectUuid().equals(root.getUuid())) {
        ComponentDto projectModule = componentDao.selectByUuid(session, moduleDto.projectUuid());
        validationMessages.add(String.format("Module \"%s\" is already part of project \"%s\"", moduleKey, projectModule.key()));
      }
    }

    private void validateBatchKey(Component component) {
      String batchKey = reportReader.readComponent(component.getRef()).getKey();
      if (!ComponentKeys.isValidModuleKey(batchKey)) {
        validationMessages.add(String.format("\"%s\" is not a valid project or module key. "
          + "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", batchKey));
      }
    }

    @CheckForNull
    private void validateBranch() {
      BatchReport.Metadata metadata = reportReader.readMetadata();
      if (!metadata.hasBranch()) {
        return;
      }
      String branch = metadata.getBranch();
      if (!ComponentKeys.isValidBranch(branch)) {
        validationMessages.add(String.format("\"%s\" is not a valid branch name. "
          + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", branch));
      }
    }

    private ComponentDto loadComponent(String componentKey) {
      ComponentDto componentDto = modulesByKey.get(componentKey);
      if (componentDto == null) {
        // Load component from key to be able to detect issue (try to analyze a module, etc.)
        return componentDao.selectNullableByKey(session, componentKey);
      }
      return componentDto;
    }
  }
}
