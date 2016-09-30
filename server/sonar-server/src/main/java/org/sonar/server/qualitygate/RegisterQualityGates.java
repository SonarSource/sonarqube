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
package org.sonar.server.qualitygate;

import org.picocontainer.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid;

import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGates implements Startable {

  private static final String BUILTIN_QUALITY_GATE = "SonarQube way";
  private static final int LEAK_PERIOD = 1;

  private final DbClient dbClient;
  private final QualityGateUpdater qualityGateUpdater;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final LoadedTemplateDao loadedTemplateDao;
  private final QualityGates qualityGates;

  public RegisterQualityGates(DbClient dbClient, QualityGateUpdater qualityGateUpdater, QualityGateConditionsUpdater qualityGateConditionsUpdater,
    LoadedTemplateDao loadedTemplateDao, QualityGates qualityGates) {
    this.dbClient = dbClient;
    this.qualityGateUpdater = qualityGateUpdater;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.loadedTemplateDao = loadedTemplateDao;
    this.qualityGates = qualityGates;
  }

  @Override
  public void start() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      if (shouldRegisterBuiltinQualityGate(dbSession)) {
        createBuiltinQualityGate(dbSession);
        registerBuiltinQualityGate(dbSession);
        dbSession.commit();
      }
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  private boolean shouldRegisterBuiltinQualityGate(DbSession dbSession) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.QUALITY_GATE_TYPE, BUILTIN_QUALITY_GATE, dbSession) == 0;
  }

  private void createBuiltinQualityGate(DbSession dbSession) {
    String ratingAValue = Integer.toString(RatingGrid.Rating.A.getIndex());
    QualityGateDto builtin = qualityGateUpdater.create(dbSession, BUILTIN_QUALITY_GATE);
    qualityGateConditionsUpdater.createCondition(dbSession, builtin.getId(),
      NEW_SECURITY_RATING_KEY, OPERATOR_GREATER_THAN, null, ratingAValue, LEAK_PERIOD);
    qualityGateConditionsUpdater.createCondition(dbSession, builtin.getId(),
      NEW_RELIABILITY_RATING_KEY, OPERATOR_GREATER_THAN, null, ratingAValue, LEAK_PERIOD);
    qualityGateConditionsUpdater.createCondition(dbSession, builtin.getId(),
      NEW_MAINTAINABILITY_RATING_KEY, OPERATOR_GREATER_THAN, null, ratingAValue, LEAK_PERIOD);
    qualityGateConditionsUpdater.createCondition(dbSession, builtin.getId(),
      NEW_COVERAGE_KEY, OPERATOR_LESS_THAN, null, "80", LEAK_PERIOD);
    qualityGates.setDefault(dbSession, builtin.getId());
  }

  private void registerBuiltinQualityGate(DbSession dbSession) {
    loadedTemplateDao.insert(new LoadedTemplateDto(BUILTIN_QUALITY_GATE, LoadedTemplateDto.QUALITY_GATE_TYPE), dbSession);
  }
}
