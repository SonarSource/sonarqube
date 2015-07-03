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
package org.sonar.server.qualitygate;

import org.picocontainer.Startable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;

public class RegisterQualityGates implements Startable {

  private static final String BUILTIN_QUALITY_GATE = "SonarQube way";

  private final QualityGates qualityGates;
  private final LoadedTemplateDao loadedTemplateDao;

  public RegisterQualityGates(QualityGates qualityGates, LoadedTemplateDao loadedTemplateDao) {
    this.qualityGates = qualityGates;
    this.loadedTemplateDao = loadedTemplateDao;
  }

  @Override
  public void start() {
    if (shouldRegisterBuiltinQualityGate()) {
      createBuiltinQualityGate();
      registerBuiltinQualityGate();
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  private boolean shouldRegisterBuiltinQualityGate() {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.QUALITY_GATE_TYPE, BUILTIN_QUALITY_GATE) == 0;
  }

  private void createBuiltinQualityGate() {
    QualityGateDto builtin = qualityGates.create(BUILTIN_QUALITY_GATE);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.BLOCKER_VIOLATIONS_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "0", null);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.CRITICAL_VIOLATIONS_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "0", 3);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.TEST_ERRORS_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "0", null);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.TEST_FAILURES_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "0", null);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.NEW_COVERAGE_KEY, QualityGateConditionDto.OPERATOR_LESS_THAN, null, "80", 3);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.OPEN_ISSUES_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, "0", null, null);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.REOPENED_ISSUES_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, "0", null, null);
    qualityGates.createCondition(builtin.getId(), CoreMetrics.SKIPPED_TESTS_KEY, QualityGateConditionDto.OPERATOR_GREATER_THAN, "0", null, null);
  }

  private void registerBuiltinQualityGate() {
    loadedTemplateDao.insert(new LoadedTemplateDto(BUILTIN_QUALITY_GATE, LoadedTemplateDto.QUALITY_GATE_TYPE));
  }
}
