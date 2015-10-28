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
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGates implements Startable {

  private static final String BUILTIN_QUALITY_GATE = "SonarQube way";
  private static final int LEAK_PERIOD = 1;
  private static final String NEW_BLOCKER_ISSUE_ERROR_THRESHOLD = "0";
  private static final String NEW_CRITICAL_ISSUE_ERROR_THRESHOLD = "0";
  private static final String DEBT_ON_NEW_CODE_ERROR_THRESHOLD = "5";
  private static final String NEW_COVERAGE_ERROR_THRESHOLD = "80";

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
    qualityGates.createCondition(builtin.getId(), NEW_BLOCKER_VIOLATIONS_KEY, OPERATOR_GREATER_THAN, null, NEW_BLOCKER_ISSUE_ERROR_THRESHOLD, LEAK_PERIOD);
    qualityGates.createCondition(builtin.getId(), NEW_CRITICAL_VIOLATIONS_KEY, OPERATOR_GREATER_THAN, null, NEW_CRITICAL_ISSUE_ERROR_THRESHOLD, LEAK_PERIOD);
    qualityGates.createCondition(builtin.getId(), NEW_SQALE_DEBT_RATIO_KEY, OPERATOR_GREATER_THAN, null, DEBT_ON_NEW_CODE_ERROR_THRESHOLD, LEAK_PERIOD);
    qualityGates.createCondition(builtin.getId(), NEW_COVERAGE_KEY, OPERATOR_LESS_THAN, null, NEW_COVERAGE_ERROR_THRESHOLD, LEAK_PERIOD);
    qualityGates.setDefault(builtin.getId());
  }

  private void registerBuiltinQualityGate() {
    loadedTemplateDao.insert(new LoadedTemplateDto(BUILTIN_QUALITY_GATE, LoadedTemplateDto.QUALITY_GATE_TYPE));
  }
}
