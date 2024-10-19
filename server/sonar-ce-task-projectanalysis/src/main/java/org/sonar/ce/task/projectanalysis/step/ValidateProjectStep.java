/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentVisitor;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;

import static java.lang.String.format;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.component.ComponentKeys.ALLOWED_CHARACTERS_MESSAGE;
import static org.sonar.core.component.ComponentKeys.isValidProjectKey;

public class ValidateProjectStep implements ComputationStep {
  private static final Joiner MESSAGES_JOINER = Joiner.on("\n  o ");

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public ValidateProjectStep(DbClient dbClient, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Component root = treeRootHolder.getRoot();
      ValidateProjectsVisitor visitor = new ValidateProjectsVisitor(dbSession, dbClient.componentDao());
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
    private final List<String> validationMessages = new ArrayList<>();

    public ValidateProjectsVisitor(DbSession session, ComponentDao componentDao) {
      super(CrawlerDepthLimit.PROJECT, ComponentVisitor.Order.PRE_ORDER);
      this.session = session;
      this.componentDao = componentDao;
    }

    @Override
    public void visitProject(Component rawProject) {
      String rawProjectKey = rawProject.getKey();
      Optional<ComponentDto> baseProjectOpt = loadBaseComponent(rawProjectKey);
      if (baseProjectOpt.isPresent()) {
        ComponentDto baseProject = baseProjectOpt.get();
        validateAnalysisDate(baseProject);
        validateProjectKey(baseProject);
      }
    }

    private void validateProjectKey(ComponentDto baseProject) {
      if (!isValidProjectKey(baseProject.getKey())) {
        validationMessages.add(format("The project key ‘%s’ contains invalid characters. %s. You should update the project key with the expected format.", baseProject.getKey(),
          ALLOWED_CHARACTERS_MESSAGE));
      }
    }

    private void validateAnalysisDate(ComponentDto baseProject) {
      Optional<SnapshotDto> snapshotDto = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(session, baseProject.uuid());
      long currentAnalysisDate = analysisMetadataHolder.getAnalysisDate();
      Long lastAnalysisDate = snapshotDto.map(SnapshotDto::getCreatedAt).orElse(null);
      if (lastAnalysisDate != null && currentAnalysisDate <= lastAnalysisDate) {
        validationMessages.add(format("Date of analysis cannot be older than the date of the last known analysis on this project. Value: \"%s\". " +
            "Latest analysis: \"%s\". It's only possible to rebuild the past in a chronological order.",
          formatDateTime(new Date(currentAnalysisDate)), formatDateTime(new Date(lastAnalysisDate))));
      }
    }

    private Optional<ComponentDto> loadBaseComponent(String rawComponentKey) {
      // Load component from key to be able to detect issue (try to analyze a module, etc.)
      if (analysisMetadataHolder.isBranch()) {
        return componentDao.selectByKeyAndBranch(session, rawComponentKey, analysisMetadataHolder.getBranch().getName());
      } else {
        return componentDao.selectByKeyAndPullRequest(session, rawComponentKey, analysisMetadataHolder.getBranch().getPullRequestKey());
      }
    }
  }
}
