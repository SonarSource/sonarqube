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

import javax.annotation.Nullable;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

/**
 * Methods from this class should be moved to {@link QualityGateUpdater} and to classes QualityGateFinder / QualityGateConditionsUpdater / etc.
 * in order to have classes with clearer responsibilities and more easily testable (without having to use too much mocks)
 */
public class QualityGates {

  public static final String SONAR_QUALITYGATE_PROPERTY = "sonar.qualitygate";

  private final DbClient dbClient;
  private final QualityGateDao dao;
  private final PropertiesDao propertiesDao;
  private final UserSession userSession;
  private final DefaultOrganizationProvider organizationProvider;

  public QualityGates(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider organizationProvider) {
    this.dbClient = dbClient;
    this.dao = dbClient.qualityGateDao();
    this.propertiesDao = dbClient.propertiesDao();
    this.userSession = userSession;
    this.organizationProvider = organizationProvider;
  }

  /**
   * Use {@link QualityGateUpdater#setDefault(DbSession, QualityGateDto)}
   * @deprecated
   */
  @Deprecated
  public void setDefault(DbSession dbSession, @Nullable Long idToUseAsDefault) {
    checkIsQualityGateAdministrator();
    if (idToUseAsDefault == null) {
      propertiesDao.deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY, dbSession);
    } else {
      QualityGateDto newDefault = getNonNullQgate(dbSession, idToUseAsDefault);
      propertiesDao.saveProperty(dbSession, new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setValue(newDefault.getId().toString()));
    }
  }

  /**
   * Use {@link QualityGateUpdater#setDefault(DbSession, QualityGateDto)}
   * @deprecated
   */
  @Deprecated
  public void setDefault(@Nullable Long idToUseAsDefault) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      setDefault(dbSession, idToUseAsDefault);
      dbSession.commit();
    }
  }

  public void dissociateProject(DbSession dbSession, ComponentDto project) {
    checkProjectAdmin(project);
    propertiesDao.deleteProjectProperty(SONAR_QUALITYGATE_PROPERTY, project.getId(), dbSession);
    dbSession.commit();
  }

  private QualityGateDto getNonNullQgate(DbSession dbSession, long id) {
    QualityGateDto qGate = dao.selectById(dbSession, id);
    if (qGate == null) {
      throw new NotFoundException("There is no quality gate with id=" + id);
    }
    return qGate;
  }

  private void checkIsQualityGateAdministrator() {
    userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, organizationProvider.get().getUuid());
  }

  private void checkProjectAdmin(ComponentDto project) {
    if (!userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, project.getOrganizationUuid())
      && !userSession.hasComponentPermission(UserRole.ADMIN, project)) {
      throw insufficientPrivilegesException();
    }
  }
}
