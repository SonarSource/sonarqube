/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.InternalPropertiesDao;

public class QualityGateDao implements Dao {
  private static final String DEFAULT_ORGANIZATION_PROPERTY_KEY = "organization.default";

  private final UuidFactory uuidFactory;
  private final InternalPropertiesDao internalPropertiesDao;

  public QualityGateDao(UuidFactory uuidFactory, InternalPropertiesDao internalPropertiesDao) {
    this.uuidFactory = uuidFactory;
    this.internalPropertiesDao = internalPropertiesDao;
  }

  public QualityGateDto insert(DbSession session, QualityGateDto newQualityGate) {
    newQualityGate.setUuid(uuidFactory.create());
    mapper(session).insertQualityGate(newQualityGate.setCreatedAt(new Date()));

    return newQualityGate;
  }

  /**
   * @deprecated drop when org are dropped
   */
  @Deprecated
  public void associate(DbSession dbSession, String uuid, OrganizationDto organization, QualityGateDto qualityGate) {
    mapper(dbSession).insertOrgQualityGate(uuid, organization.getUuid(), qualityGate.getUuid());
  }

  public void associate(DbSession dbSession, String uuid, QualityGateDto qualityGate) {
    String defaultOrganizationUuid = getDefaultOrganizationUuid(dbSession);
    mapper(dbSession).insertOrgQualityGate(uuid, defaultOrganizationUuid, qualityGate.getUuid());
  }

  public Collection<QualityGateDto> selectAll(DbSession session) {
    String defaultOrganizationUuid = getDefaultOrganizationUuid(session);
    return selectAll(session, defaultOrganizationUuid);
  }

  /**
   * @deprecated drop when org are dropped
   */
  @Deprecated
  public Collection<QualityGateDto> selectAll(DbSession session, String organizationUuid) {
    return mapper(session).selectAll(organizationUuid);
  }

  @CheckForNull
  public QualityGateDto selectByName(DbSession session, String name) {
    return mapper(session).selectByName(name);
  }

  @CheckForNull
  public QualityGateDto selectByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
  }

  @CheckForNull
  public QGateWithOrgDto selectByDefaultOrganizationAndUuid(DbSession dbSession, String qualityGateUuid) {
    String defaultOrganizationUuid = getDefaultOrganizationUuid(dbSession);
    return mapper(dbSession).selectByUuidAndOrganization(qualityGateUuid, defaultOrganizationUuid);
  }

  /**
   * @deprecated drop when org are dropped
   */
  @CheckForNull
  @Deprecated
  public QGateWithOrgDto selectByOrganizationAndUuid(DbSession dbSession, OrganizationDto organization, String qualityGateUuid) {
    return mapper(dbSession).selectByUuidAndOrganization(qualityGateUuid, organization.getUuid());
  }

  @CheckForNull
  public QGateWithOrgDto selectByDefaultOrganizationAndName(DbSession session, String name) {
    String defaultOrganizationUuid = getDefaultOrganizationUuid(session);
    return mapper(session).selectByNameAndOrganization(name, defaultOrganizationUuid);
  }

  public void delete(QualityGateDto qGate, DbSession session) {
    mapper(session).delete(qGate.getUuid());
    mapper(session).deleteOrgQualityGatesByQualityGateUuid(qGate.getUuid());
  }

  public void deleteByUuids(DbSession session, Collection<String> uuids) {
    QualityGateMapper mapper = mapper(session);
    DatabaseUtils.executeLargeUpdates(uuids, mapper::deleteByUuids);
  }

  public void deleteOrgQualityGatesByOrganization(DbSession session, OrganizationDto organization) {
    mapper(session).deleteOrgQualityGatesByOrganization(organization.getUuid());
  }

  public void update(QualityGateDto qGate, DbSession session) {
    mapper(session).update(qGate.setUpdatedAt(new Date()));
  }

  public void ensureOneBuiltInQualityGate(DbSession dbSession, String builtInName) {
    mapper(dbSession).ensureOneBuiltInQualityGate(builtInName);
  }

  public QualityGateDto selectBuiltIn(DbSession dbSession) {
    return mapper(dbSession).selectBuiltIn();
  }

  private static QualityGateMapper mapper(DbSession session) {
    return session.getMapper(QualityGateMapper.class);
  }

  public QualityGateDto selectByProjectUuid(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }

  private String getDefaultOrganizationUuid(DbSession dbSession) {
    return internalPropertiesDao.selectByKey(dbSession, DEFAULT_ORGANIZATION_PROPERTY_KEY)
      .orElseThrow(() -> new IllegalStateException("Default organization does not exist."));
  }
}
