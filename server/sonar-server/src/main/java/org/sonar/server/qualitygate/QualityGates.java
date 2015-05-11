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

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;

/**
 * @since 4.3
 */
public class QualityGates {

  public static final String SONAR_QUALITYGATE_PROPERTY = "sonar.qualitygate";

  private final QualityGateDao dao;
  private final QualityGateConditionDao conditionDao;
  private final MetricFinder metricFinder;
  private final PropertiesDao propertiesDao;
  private final ComponentDao componentDao;
  private final MyBatis myBatis;
  private final UserSession userSession;

  public QualityGates(QualityGateDao dao, QualityGateConditionDao conditionDao, MetricFinder metricFinder, PropertiesDao propertiesDao, ComponentDao componentDao,
    MyBatis myBatis, UserSession userSession) {
    this.dao = dao;
    this.conditionDao = conditionDao;
    this.metricFinder = metricFinder;
    this.propertiesDao = propertiesDao;
    this.componentDao = componentDao;
    this.myBatis = myBatis;
    this.userSession = userSession;
  }

  public QualityGateDto create(String name) {
    checkPermission();
    validateQualityGate(null, name);
    QualityGateDto newQualityGate = new QualityGateDto().setName(name);
    dao.insert(newQualityGate);
    return newQualityGate;
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
    SqlSession session = myBatis.openSession(false);
    try {
      dao.insert(destinationGate, session);
      for (QualityGateConditionDto sourceCondition : conditionDao.selectForQualityGate(sourceId, session)) {
        conditionDao.insert(new QualityGateConditionDto().setQualityGateId(destinationGate.getId())
          .setMetricId(sourceCondition.getMetricId()).setOperator(sourceCondition.getOperator())
          .setWarningThreshold(sourceCondition.getWarningThreshold()).setErrorThreshold(sourceCondition.getErrorThreshold()).setPeriod(sourceCondition.getPeriod()),
          session
          );
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return destinationGate;
  }

  public Collection<QualityGateDto> list() {
    return dao.selectAll();
  }

  public void delete(long idToDelete) {
    checkPermission();
    QualityGateDto qGate = getNonNullQgate(idToDelete);
    SqlSession session = myBatis.openSession(false);
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

  public void setDefault(@Nullable Long idToUseAsDefault) {
    checkPermission();
    if (idToUseAsDefault == null) {
      propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY);
    } else {
      QualityGateDto newDefault = getNonNullQgate(idToUseAsDefault);
      propertiesDao.setProperty(new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setValue(newDefault.getId().toString()));
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

  public QualityGateConditionDto createCondition(long qGateId, String metricKey, String operator,
    @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    checkPermission();
    getNonNullQgate(qGateId);
    Metric metric = getNonNullMetric(metricKey);
    validateCondition(metric, operator, warningThreshold, errorThreshold, period);
    QualityGateConditionDto newCondition = new QualityGateConditionDto().setQualityGateId(qGateId)
      .setMetricId(metric.getId()).setMetricKey(metric.getKey())
      .setOperator(operator).setWarningThreshold(warningThreshold).setErrorThreshold(errorThreshold).setPeriod(period);
    conditionDao.insert(newCondition);
    return newCondition;
  }

  public QualityGateConditionDto updateCondition(long condId, String metricKey, String operator,
    @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    checkPermission();
    QualityGateConditionDto condition = getNonNullCondition(condId);
    Metric metric = getNonNullMetric(metricKey);
    validateCondition(metric, operator, warningThreshold, errorThreshold, period);
    condition.setMetricId(metric.getId()).setMetricKey(metric.getKey())
      .setOperator(operator).setWarningThreshold(warningThreshold).setErrorThreshold(errorThreshold).setPeriod(period);
    conditionDao.update(condition);
    return condition;
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
    DbSession session = myBatis.openSession(false);
    try {
      getNonNullQgate(qGateId);
      checkPermission(projectId, session);
      propertiesDao.setProperty(new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setResourceId(projectId).setValue(qGateId.toString()));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void dissociateProject(Long qGateId, Long projectId) {
    DbSession session = myBatis.openSession(false);
    try {
      getNonNullQgate(qGateId);
      checkPermission(projectId, session);
      propertiesDao.deleteProjectProperty(SONAR_QUALITYGATE_PROPERTY, projectId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<Metric> gateMetrics() {
    return Collections2.filter(metricFinder.findAll(), new Predicate<Metric>() {
      @Override
      public boolean apply(Metric metric) {
        return isAvailableForInit(metric);
      }
    });
  }

  public boolean currentUserHasWritePermission() {
    boolean hasWritePermission = false;
    try {
      checkPermission();
      hasWritePermission = true;
    } catch (ServerException unallowed) {
      // Ignored
    }
    return hasWritePermission;
  }

  private void validateCondition(Metric metric, String operator, @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    Errors errors = new Errors();
    validateMetric(metric, errors);
    checkOperator(metric, operator, errors);
    checkThresholds(warningThreshold, errorThreshold, errors);
    checkPeriod(metric, period, errors);
    if (!errors.isEmpty()) {
      throw new BadRequestException(errors);
    }
  }

  private void checkPeriod(Metric metric, @Nullable Integer period, Errors errors) {
    if (period == null) {
      errors.check(!metric.getKey().startsWith("new_"), "A period must be selected for differential metrics.");

    } else {
      errors.check(period >= 1 && period <= 5, "Valid periods are integers between 1 and 5 (included).");
    }
  }

  private void checkThresholds(@Nullable String warningThreshold, @Nullable String errorThreshold, Errors errors) {
    errors.check(warningThreshold != null || errorThreshold != null, "At least one threshold (warning, error) must be set.");
  }

  private void checkOperator(Metric metric, String operator, Errors errors) {
    errors
      .check(QualityGateConditionDto.isOperatorAllowed(operator, metric.getType()), String.format("Operator %s is not allowed for metric type %s.", operator, metric.getType()));
  }

  private void validateMetric(Metric metric, Errors errors) {
    errors.check(isAlertable(metric), String.format("Metric '%s' cannot be used to define a condition.", metric.getKey()));
  }

  private boolean isAvailableForInit(Metric metric) {
    return !metric.isDataType() && !CoreMetrics.ALERT_STATUS.equals(metric) && ValueType.RATING != metric.getType();
  }

  private boolean isAlertable(Metric metric) {
    return isAvailableForInit(metric) && BooleanUtils.isFalse(metric.isHidden());
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
    QualityGateDto qGate = dao.selectById(id);
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

  private Metric getNonNullMetric(String metricKey) {
    Metric metric = metricFinder.findByKey(metricKey);
    if (metric == null) {
      throw new NotFoundException("There is no metric with key=" + metricKey);
    }
    return metric;
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
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private void checkPermission(Long projectId, DbSession session) {
    ComponentDto project = componentDao.getById(projectId, session);
    if (!userSession.hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN) && !userSession.hasProjectPermission(UserRole.ADMIN, project.key())) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }
}
