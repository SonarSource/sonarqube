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
package org.sonar.server.qualityprofile;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Create, delete and set as default profile.
 */
public class QProfileFactory {

  private final DbClient db;
  private final UuidFactory uuidFactory;
  private final System2 system2;
  private final ActiveRuleIndexer activeRuleIndexer;

  public QProfileFactory(DbClient db, UuidFactory uuidFactory, System2 system2, ActiveRuleIndexer activeRuleIndexer) {
    this.db = db;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  // ------------- CREATION

  QualityProfileDto getOrCreate(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    requireNonNull(organization);
    QualityProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(organization, name.getName(), name.getLanguage(), dbSession);
    if (profile == null) {
      profile = doCreate(dbSession, organization, name, false);
    }
    return profile;
  }

  /**
   * Create the quality profile in DB with the specified name.
   *
   * @throws BadRequestException if a quality profile with the specified name already exists
   */
  public QualityProfileDto checkAndCreate(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    requireNonNull(organization);
    QualityProfileDto dto = db.qualityProfileDao().selectByNameAndLanguage(organization, name.getName(), name.getLanguage(), dbSession);
    checkRequest(dto == null, "Quality profile already exists: %s", name);
    return doCreate(dbSession, organization, name, false);
  }

  /**
   * Create the quality profile in DB with the specified name.
   *
   * A DB error will be thrown if the quality profile already exists.
   */
  public QualityProfileDto create(DbSession dbSession, OrganizationDto organization, QProfileName name, boolean isDefault) {
    return doCreate(dbSession, requireNonNull(organization), name, isDefault);
  }

  private static OrganizationDto requireNonNull(@Nullable OrganizationDto organization) {
    Objects.requireNonNull(organization, "Organization is required, when creating a quality profile.");
    return organization;
  }

  private QualityProfileDto doCreate(DbSession dbSession, OrganizationDto organization, QProfileName name, boolean isDefault) {
    if (StringUtils.isEmpty(name.getName())) {
      throw BadRequestException.create("quality_profiles.profile_name_cant_be_blank");
    }
    Date now = new Date(system2.now());
    QualityProfileDto dto = QualityProfileDto.createFor(uuidFactory.create())
      .setName(name.getName())
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(name.getLanguage())
      .setDefault(isDefault)
      .setRulesUpdatedAtAsDate(now);
    db.qualityProfileDao().insert(dbSession, dto);
    return dto;
  }

  // ------------- DELETION

  /**
   * Deletes the profiles with specified keys from database and Elasticsearch.
   * All related information are deleted. The profiles marked as "default"
   * are deleted too. Deleting a parent profile does not delete descendants
   * if their keys are not listed.
   */
  public void deleteByKeys(DbSession dbSession, Collection<String> profileKeys) {
    if (!profileKeys.isEmpty()) {
      db.qualityProfileDao().deleteProjectAssociationsByProfileKeys(dbSession, profileKeys);
      db.activeRuleDao().deleteParametersByProfileKeys(dbSession, profileKeys);
      db.activeRuleDao().deleteByProfileKeys(dbSession, profileKeys);
      db.qProfileChangeDao().deleteByProfileKeys(dbSession, profileKeys);
      db.qualityProfileDao().deleteByKeys(dbSession, profileKeys);
      dbSession.commit();
      activeRuleIndexer.deleteByProfileKeys(profileKeys);
    }
  }

}
