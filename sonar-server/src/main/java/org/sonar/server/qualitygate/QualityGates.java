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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 4.3
 */
public class QualityGates {

  private final QualityGateDao dao;

  public QualityGates(QualityGateDao dao) {
    this.dao = dao;
  }

  public QualityGateDto create(String name) {
    checkPermission(UserSession.get());
    validateQualityGate(null, name);
    QualityGateDto newQualityGate = new QualityGateDto().setName(name);
    dao.insert(newQualityGate);
    return newQualityGate;
  }

  public Collection<QualityGateDto> list() {
    return dao.selectAll();
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
