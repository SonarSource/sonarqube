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

import com.google.common.base.Strings;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

/**
 * Methods from this class should be moved to {@link QualityGateUpdater} and to new classes QualityGateFinder / QualityGateConditionsUpdater / etc.
 * in order to have classes with clearer responsibilities and more easily testable (without having to use too much mocks)
 */
public class QualityGates {

  public static final String SONAR_QUALITYGATE_PROPERTY = "sonar.qualitygate";

  private final DbClient dbClient;
  private final QualityGateDao dao;
  private final QualityGateConditionDao conditionDao;
  private final MetricFinder metricFinder;
  private final PropertiesDao propertiesDao;
  private final ComponentDao componentDao;
  private final UserSession userSession;

  public QualityGates(DbClient dbClient, MetricFinder metricFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.dao = dbClient.qualityGateDao();
    this.conditionDao = dbClient.gateConditionDao();
    this.metricFinder = metricFinder;
    this.propertiesDao = dbClient.propertiesDao();
    this.componentDao = dbClient.componentDao();
    this.userSession = userSession;
  }

  public QualityGateDto get(Long qGateId) {
    return getNonNullQgate(qGateId);
  }

  public QualityGateDto get(String qGateName) {
    return getNonNullQgate(qGateName);
  }

  public QualityGateDto rename(long idToRename, String name) {
    checkPermission();
    QualityGateDto toRename = getNonNullQgate(idToRename);
    validateQualityGate(idToRename, name);
    toRename.setName(name);
    dao.update(toRename);
    return toRename;
  }

  public QualityGateDto copy(long sourceId, String destinationName) {
    checkPermission();
    getNonNullQgate(sourceId);
    validateQualityGate(null, destinationName);
    QualityGateDto destinationGate = new QualityGateDto().setName(destinationName);
    DbSession dbSession = dbClient.openSession(false);
    try {
      dao.insert(dbSession, destinationGate);
      for (QualityGateConditionDto sourceCondition : conditionDao.selectForQualityGate(sourceId, dbSession)) {
        conditionDao.insert(new QualityGateConditionDto().setQualityGateId(destinationGate.getId())
          .setMetricId(sourceCondition.getMetricId()).setOperator(sourceCondition.getOperator())
          .setWarningThreshold(sourceCondition.getWarningThreshold()).setErrorThreshold(sourceCondition.getErrorThreshold()).setPeriod(sourceCondition.getPeriod()),
          dbSession);
      }
      dbSession.commit();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
    return destinationGate;
  }

  public Collection<QualityGateDto> list() {
    return dao.selectAll();
  }

  public void delete(long idToDelete) {
    checkPermission();
    QualityGateDto qGate = getNonNullQgate(idToDelete);
    DbSession session = dbClient.openSession(false);
    try {
      if (isDefault(qGate)) {
        propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY, session);
      }
      propertiesDao.deleteProjectProperties(SONAR_QUALITYGATE_PROPERTY, Long.toString(idToDelete), session);
      dao.delete(qGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void setDefault(DbSession dbSession, @Nullable Long idToUseAsDefault) {
    checkPermission();
    if (idToUseAsDefault == null) {
      propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY, dbSession);
    } else {
      QualityGateDto newDefault = getNonNullQgate(dbSession, idToUseAsDefault);
      propertiesDao.saveProperty(dbSession, new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setValue(newDefault.getId().toString()));
    }
  }

  public void setDefault(@Nullable Long idToUseAsDefault) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      setDefault(dbSession, idToUseAsDefault);
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @CheckForNull
  public QualityGateDto getDefault() {
    Long defaultId = getDefaultId();
    if (defaultId == null) {
      return null;
    } else {
      return dao.selectById(defaultId);
    }
  }

  public Collection<QualityGateConditionDto> listConditions(long qGateId) {
    Collection<QualityGateConditionDto> conditionsForGate = conditionDao.selectForQualityGate(qGateId);
    for (QualityGateConditionDto condition : conditionsForGate) {
      Metric metric = metricFinder.findById((int) condition.getMetricId());
      if (metric == null) {
        throw new IllegalStateException("Could not find metric with id " + condition.getMetricId());
      }
      condition.setMetricKey(metric.getKey());
    }
    return conditionsForGate;
  }

  public void deleteCondition(Long condId) {
    checkPermission();
    conditionDao.delete(getNonNullCondition(condId));
  }

  public void associateProject(Long qGateId, Long projectId) {
    DbSession session = dbClient.openSession(false);
    try {
      getNonNullQgate(qGateId);
      checkPermission(projectId, session);
      propertiesDao.saveProperty(new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setResourceId(projectId).setValue(qGateId.toString()));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void dissociateProject(Long qGateId, Long projectId) {
    DbSession session = dbClient.openSession(false);
    try {
      getNonNullQgate(qGateId);
      checkPermission(projectId, session);
      propertiesDao.deleteProjectProperty(SONAR_QUALITYGATE_PROPERTY, projectId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private boolean isDefault(QualityGateDto qGate) {
    return qGate.getId().equals(getDefaultId());
  }

  private Long getDefaultId() {
    PropertyDto defaultQgate = propertiesDao.selectGlobalProperty(SONAR_QUALITYGATE_PROPERTY);
    if (defaultQgate == null || StringUtils.isBlank(defaultQgate.getValue())) {
      return null;
    } else {
      return Long.valueOf(defaultQgate.getValue());
    }
  }

  private QualityGateDto getNonNullQgate(long id) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return getNonNullQgate(dbSession, id);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private QualityGateDto getNonNullQgate(DbSession dbSession, long id) {
    QualityGateDto qGate = dao.selectById(dbSession, id);
    if (qGate == null) {
      throw new NotFoundException("There is no quality gate with id=" + id);
    }
    return qGate;
  }

  private QualityGateDto getNonNullQgate(String name) {
    QualityGateDto qGate = dao.selectByName(name);
    if (qGate == null) {
      throw new NotFoundException("There is no quality gate with name=" + name);
    }
    return qGate;
  }

  private QualityGateConditionDto getNonNullCondition(long id) {
    QualityGateConditionDto condition = conditionDao.selectById(id);
    if (condition == null) {
      throw new NotFoundException("There is no condition with id=" + id);
    }
    return condition;
  }

  private void validateQualityGate(@Nullable Long updatingQgateId, @Nullable String name) {
    Errors errors = new Errors();
    if (Strings.isNullOrEmpty(name)) {
      errors.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      checkQgateNotAlreadyExists(updatingQgateId, name, errors);
    }
    if (!errors.isEmpty()) {
      throw new BadRequestException(errors);
    }
  }

  private void checkQgateNotAlreadyExists(@Nullable Long updatingQgateId, String name, Errors errors) {
    QualityGateDto existingQgate = dao.selectByName(name);
    boolean isModifyingCurrentQgate = updatingQgateId != null && existingQgate != null && existingQgate.getId().equals(updatingQgateId);
    errors.check(isModifyingCurrentQgate || existingQgate == null, Validation.IS_ALREADY_USED_MESSAGE, "Name");
  }

  private void checkPermission() {
    userSession.checkPermission(GlobalPermissions.QUALITY_GATE_ADMIN);
  }

  private void checkPermission(Long projectId, DbSession session) {
    ComponentDto project = componentDao.selectOrFailById(session, projectId);
    if (!userSession.hasPermission(GlobalPermissions.QUALITY_GATE_ADMIN)
      && !userSession.hasComponentUuidPermission(UserRole.ADMIN, project.uuid())) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }
}
