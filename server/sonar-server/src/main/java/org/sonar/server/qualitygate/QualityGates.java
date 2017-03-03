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
package org.sonar.server.qualitygate;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.checkRequest;

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
  private final UserSession userSession;

  public QualityGates(DbClient dbClient, MetricFinder metricFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.dao = dbClient.qualityGateDao();
    this.conditionDao = dbClient.gateConditionDao();
    this.metricFinder = metricFinder;
    this.propertiesDao = dbClient.propertiesDao();
    this.userSession = userSession;
  }

  public QualityGateDto get(Long qGateId) {
    return getNonNullQgate(qGateId);
  }

  public QualityGateDto get(String qGateName) {
    return getNonNullQgate(qGateName);
  }

  public QualityGateDto rename(long idToRename, String name) {
    checkIsSystemAdministrator();
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto toRename = getNonNullQgate(idToRename);
      validateQualityGate(dbSession, idToRename, name);
      toRename.setName(name);
      dao.update(toRename, dbSession);
      dbSession.commit();
      return toRename;
    }
  }

  public QualityGateDto copy(long sourceId, String destinationName) {
    checkIsSystemAdministrator();
    getNonNullQgate(sourceId);
    try (DbSession dbSession = dbClient.openSession(false)) {
      validateQualityGate(dbSession, null, destinationName);
      QualityGateDto destinationGate = new QualityGateDto().setName(destinationName);
      dao.insert(dbSession, destinationGate);
      for (QualityGateConditionDto sourceCondition : conditionDao.selectForQualityGate(dbSession, sourceId)) {
        conditionDao.insert(new QualityGateConditionDto().setQualityGateId(destinationGate.getId())
          .setMetricId(sourceCondition.getMetricId()).setOperator(sourceCondition.getOperator())
          .setWarningThreshold(sourceCondition.getWarningThreshold()).setErrorThreshold(sourceCondition.getErrorThreshold()).setPeriod(sourceCondition.getPeriod()),
          dbSession);
      }
      dbSession.commit();
      return destinationGate;
    }
  }

  public Collection<QualityGateDto> list() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dao.selectAll(dbSession);
    }
  }

  public void delete(long idToDelete) {
    checkIsSystemAdministrator();
    QualityGateDto qGate = getNonNullQgate(idToDelete);
    try (DbSession session = dbClient.openSession(false)) {
      if (isDefault(qGate)) {
        propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY, session);
      }
      propertiesDao.deleteProjectProperties(SONAR_QUALITYGATE_PROPERTY, Long.toString(idToDelete), session);
      dao.delete(qGate, session);
      session.commit();
    }
  }

  public void setDefault(DbSession dbSession, @Nullable Long idToUseAsDefault) {
    checkIsSystemAdministrator();
    if (idToUseAsDefault == null) {
      propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY, dbSession);
    } else {
      QualityGateDto newDefault = getNonNullQgate(dbSession, idToUseAsDefault);
      propertiesDao.saveProperty(dbSession, new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setValue(newDefault.getId().toString()));
    }
  }

  public void setDefault(@Nullable Long idToUseAsDefault) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      setDefault(dbSession, idToUseAsDefault);
      dbSession.commit();
    }
  }

  @CheckForNull
  public QualityGateDto getDefault() {
    Long defaultId = getDefaultId();
    if (defaultId == null) {
      return null;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dao.selectById(dbSession, defaultId);
    }
  }

  public Collection<QualityGateConditionDto> listConditions(long qGateId) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Collection<QualityGateConditionDto> conditionsForGate = conditionDao.selectForQualityGate(dbSession, qGateId);
      for (QualityGateConditionDto condition : conditionsForGate) {
        Metric metric = metricFinder.findById((int) condition.getMetricId());
        if (metric == null) {
          throw new IllegalStateException("Could not find metric with id " + condition.getMetricId());
        }
        condition.setMetricKey(metric.getKey());
      }
      return conditionsForGate;
    }
  }

  public void deleteCondition(Long condId) {
    checkIsSystemAdministrator();
    try (DbSession dbSession = dbClient.openSession(false)) {
      conditionDao.delete(getNonNullCondition(dbSession, condId), dbSession);
      dbSession.commit();
    }
  }

  public void dissociateProject(DbSession dbSession, Long qGateId, ComponentDto project) {
    getNonNullQgate(qGateId);
    checkProjectAdmin(project);
    propertiesDao.deleteProjectProperty(SONAR_QUALITYGATE_PROPERTY, project.getId(), dbSession);
    dbSession.commit();
  }

  private boolean isDefault(QualityGateDto qGate) {
    return qGate.getId().equals(getDefaultId());
  }

  private Long getDefaultId() {
    PropertyDto defaultQgate = propertiesDao.selectGlobalProperty(SONAR_QUALITYGATE_PROPERTY);
    if (defaultQgate == null || StringUtils.isBlank(defaultQgate.getValue())) {
      return null;
    }
    return Long.valueOf(defaultQgate.getValue());
  }

  private QualityGateDto getNonNullQgate(long id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return getNonNullQgate(dbSession, id);
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
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qGate = dao.selectByName(dbSession, name);
      if (qGate == null) {
        throw new NotFoundException("There is no quality gate with name=" + name);
      }
      return qGate;
    }
  }

  private QualityGateConditionDto getNonNullCondition(DbSession dbSession, long id) {
    QualityGateConditionDto condition = conditionDao.selectById(id, dbSession);
    if (condition == null) {
      throw new NotFoundException("There is no condition with id=" + id);
    }
    return condition;
  }

  private void validateQualityGate(DbSession dbSession, @Nullable Long updatingQgateId, @Nullable String name) {
    List<String> errors = new ArrayList<>();
    if (Strings.isNullOrEmpty(name)) {
      errors.add(format(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      checkQgateNotAlreadyExists(dbSession, updatingQgateId, name, errors);
    }
    checkRequest(errors.isEmpty(), errors);
  }

  private void checkQgateNotAlreadyExists(DbSession dbSession, @Nullable Long updatingQgateId, String name, List<String> errors) {
    QualityGateDto existingQgate = dao.selectByName(dbSession, name);
    boolean isModifyingCurrentQgate = updatingQgateId != null && existingQgate != null && existingQgate.getId().equals(updatingQgateId);
    if (!isModifyingCurrentQgate && existingQgate != null) {
      errors.add(format(Validation.IS_ALREADY_USED_MESSAGE, "Name"));
    }
  }

  private void checkIsSystemAdministrator() {
    userSession.checkIsSystemAdministrator();
  }

  private void checkProjectAdmin(ComponentDto project) {
    if (!userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, project.getOrganizationUuid())
      && !userSession.hasComponentPermission(UserRole.ADMIN, project)) {
      throw insufficientPrivilegesException();
    }
  }
}
