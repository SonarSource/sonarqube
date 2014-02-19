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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
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

  private final PropertiesDao propertiesDao;

  public QualityGates(QualityGateDao dao, PropertiesDao propertiesDao) {
    this.dao = dao;
    this.propertiesDao = propertiesDao;
  }

  public QualityGateDto create(String name) {
    checkPermission(UserSession.get());
    validateQualityGate(null, name);
    QualityGateDto newQualityGate = new QualityGateDto().setName(name);
    dao.insert(newQualityGate);
    return newQualityGate;
  }

  public QualityGateDto rename(long idToRename, String name) {
    checkPermission(UserSession.get());
    QualityGateDto toRename = getNonNull(idToRename);
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
    QualityGateDto qGate = getNonNull(idToDelete);
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
      QualityGateDto newDefault = getNonNull(idToUseAsDefault);
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

  private QualityGateDto getNonNull(long id) {
    QualityGateDto qGate = dao.selectById(id);
    if (qGate == null) {
      throw new NotFoundException("There is no quality gate with id=" + id);
    }
    return qGate;
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
