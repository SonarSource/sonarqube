/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.BadRequestException.Message;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 4.3
 */
public class QualityGates {

  public static final String SONAR_QUALITYGATE_PROPERTY = "sonar.qualitygate";

  private final QualityGateDao dao;

  private final QualityGateConditionDao conditionDao;

  private final MetricFinder metricFinder;

  private final PropertiesDao propertiesDao;

  public QualityGates(QualityGateDao dao, QualityGateConditionDao conditionDao, MetricFinder metricFinder, PropertiesDao propertiesDao) {
    this.dao = dao;
    this.conditionDao = conditionDao;
    this.metricFinder = metricFinder;
    this.propertiesDao = propertiesDao;
  }

  public QualityGateDto create(String name) {
    checkPermission(UserSession.get());
    validateQualityGate(null, name);
    QualityGateDto newQualityGate = new QualityGateDto().setName(name);
    dao.insert(newQualityGate);
    return newQualityGate;
  }

  public QualityGateDto get(Long parseId) {
    return getNonNullQgate(parseId);
  }

  public QualityGateDto rename(long idToRename, String name) {
    checkPermission(UserSession.get());
    QualityGateDto toRename = getNonNullQgate(idToRename);
    validateQualityGate(idToRename, name);
    toRename.setName(name);
    dao.update(toRename);
    return toRename;
  }

  public Collection<QualityGateDto> list() {
    return dao.selectAll();
  }

  public void delete(long idToDelete) {
    checkPermission(UserSession.get());
    QualityGateDto qGate = getNonNullQgate(idToDelete);
    if (isDefault(qGate)) {
      throw new BadRequestException("Impossible to delete default quality gate.");
    }
    dao.delete(qGate);
  }

  public void setDefault(@Nullable Long idToUseAsDefault) {
    checkPermission(UserSession.get());
    if (idToUseAsDefault == null) {
      propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY);
    } else {
      QualityGateDto newDefault = getNonNullQgate(idToUseAsDefault);
      propertiesDao.setProperty(new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setValue(newDefault.getName()));
    }
  }

  @CheckForNull
  public QualityGateDto getDefault() {
    String defaultName = getDefaultName();
    if (defaultName == null) {
      return null;
    } else {
      return dao.selectByName(defaultName);
    }
  }

  public QualityGateConditionDto createCondition(long qGateId, String metricKey, String operator,
    @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    checkPermission(UserSession.get());
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
    checkPermission(UserSession.get());
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
    for (QualityGateConditionDto condition: conditionsForGate) {
      condition.setMetricKey(metricFinder.findById((int) condition.getMetricId()).getKey());
    }
    return conditionsForGate;
  }

  public void deleteCondition(Long condId) {
    checkPermission(UserSession.get());
    conditionDao.delete(getNonNullCondition(condId));
  }

  private void validateCondition(Metric metric, String operator, String warningThreshold, String errorThreshold, Integer period) {
    List<Message> validationMessages = Lists.newArrayList();
    validateMetric(metric, validationMessages);
    validateOperator(metric, operator, validationMessages);
    validateThresholds(warningThreshold, errorThreshold, validationMessages);
    validatePeriod(metric, period, validationMessages);
    if (!validationMessages.isEmpty()) {
      throw BadRequestException.of(validationMessages);
    }
  }

  private void validatePeriod(Metric metric, Integer period, List<Message> validationMessages) {
    if (period == null) {
      if (metric.getKey().startsWith("new_")) {
        validationMessages.add(Message.of("A period must be selected for differential metrics."));
      }
    } else if (period < 1 || period > 3) {
      validationMessages.add(Message.of("Valid periods are 1, 2 and 3."));
    }
  }

  private void validateThresholds(String warningThreshold, String errorThreshold, List<Message> validationMessages) {
    if (warningThreshold == null && errorThreshold == null) {
      validationMessages.add(Message.of("At least one threshold (warning, error) must be set."));
    }
  }

  private void validateOperator(Metric metric, String operator, List<Message> validationMessages) {
    if (!QualityGateConditionDto.isOperatorAllowed(operator, metric.getType())) {
      validationMessages.add(Message.of(String.format("Operator %s is not allowed for metric type %s.", operator, metric.getType())));
    }
  }

  private void validateMetric(Metric metric, List<Message> validationMessages) {
    if (metric.isDataType() || metric.isHidden() || CoreMetrics.ALERT_STATUS.equals(metric) || ValueType.RATING == metric.getType()) {
      validationMessages.add(Message.of(String.format("Metric '%s' cannot be used to define a condition.", metric.getKey())));
    }
  }

  private boolean isDefault(QualityGateDto qGate) {
    return qGate.getName().equals(getDefaultName());
  }

  private String getDefaultName() {
    PropertyDto defaultQgate = propertiesDao.selectGlobalProperty(SONAR_QUALITYGATE_PROPERTY);
    if (defaultQgate == null || StringUtils.isBlank(defaultQgate.getValue())) {
      return null;
    } else {
      return defaultQgate.getValue();
    }
  }

  private QualityGateDto getNonNullQgate(long id) {
    QualityGateDto qGate = dao.selectById(id);
    if (qGate == null) {
      throw new NotFoundException("There is no quality gate with id=" + id);
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
    List<BadRequestException.Message> messages = newArrayList();
    if (Strings.isNullOrEmpty(name)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      messages.addAll(checkQgateNotAlreadyExists(updatingQgateId, name));
    }
    if (!messages.isEmpty()) {
      throw BadRequestException.of(messages);
    }
  }

  private Collection<BadRequestException.Message> checkQgateNotAlreadyExists(@Nullable Long updatingQgateId, String name) {
    QualityGateDto existingQgate = dao.selectByName(name);
    boolean isModifyingCurrentQgate = updatingQgateId != null && existingQgate != null && existingQgate.getId().equals(updatingQgateId);
    if (!isModifyingCurrentQgate && existingQgate != null) {
      return Collections.singleton(BadRequestException.Message.ofL10n(Validation.IS_ALREADY_USED_MESSAGE, "Name"));
    }
    return Collections.emptySet();
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}
