/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Optional;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.qualitygate.MutableQualityGateHolder;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateService;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * This step retrieves the QualityGate and stores it in
 * {@link MutableQualityGateHolder}.
 */
public class LoadQualityGateStep implements ComputationStep {
  private static final Logger LOGGER = Loggers.get(LoadQualityGateStep.class);

  private static final String PROPERTY_QUALITY_GATE = "sonar.qualitygate";

  private final TreeRootHolder treeRootHolder;
  private final ConfigurationRepository configRepository;
  private final QualityGateService qualityGateService;
  private final MutableQualityGateHolder qualityGateHolder;

  public LoadQualityGateStep(TreeRootHolder treeRootHolder, ConfigurationRepository settingsRepository,
    QualityGateService qualityGateService, MutableQualityGateHolder qualityGateHolder) {
    this.treeRootHolder = treeRootHolder;
    this.configRepository = settingsRepository;
    this.qualityGateService = qualityGateService;
    this.qualityGateHolder = qualityGateHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          executeForProject(project);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    String projectKey = project.getKey();
    Configuration config = configRepository.getConfiguration(project);
    String qualityGateSetting = config.get(PROPERTY_QUALITY_GATE).orElse(null);

    if (StringUtils.isBlank(qualityGateSetting)) {
      LOGGER.debug("No quality gate is configured for project " + projectKey);
      qualityGateHolder.setNoQualityGate();
      return;
    }

    try {
      long qualityGateId = Long.parseLong(qualityGateSetting);
      Optional<QualityGate> qualityGate = qualityGateService.findById(qualityGateId);
      if (qualityGate.isPresent()) {
        qualityGateHolder.setQualityGate(qualityGate.get());
      } else {
        qualityGateHolder.setNoQualityGate();
      }
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
        String.format("Unsupported value (%s) in property %s", qualityGateSetting, PROPERTY_QUALITY_GATE),
        e);
    }
  }

  @Override
  public String getDescription() {
    return "Load Quality gate";
  }
}
