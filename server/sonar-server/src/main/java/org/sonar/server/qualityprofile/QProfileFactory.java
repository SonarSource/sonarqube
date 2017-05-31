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
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static com.google.common.base.Preconditions.checkArgument;
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

  private static OrganizationDto requireNonNull(@Nullable OrganizationDto organization) {
    Objects.requireNonNull(organization, "Organization is required, when creating a quality profile.");
    return organization;
  }

  QProfileDto getOrCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    requireNonNull(organization);
    QProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, name.getName(), name.getLanguage());
    if (profile == null) {
      profile = doCreate(dbSession, organization, name, false, false);
    } else {
      checkArgument(!profile.isBuiltIn(), "Operation forbidden for built-in Quality Profile '%s' with language '%s'", profile.getName(), profile.getLanguage());
    }

    return profile;
  }

  /**
   * Create the quality profile in DB with the specified name.
   *
   * @throws BadRequestException if a quality profile with the specified name already exists
   */
  public QProfileDto checkAndCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    requireNonNull(organization);
    QProfileDto dto = db.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, name.getName(), name.getLanguage());
    checkRequest(dto == null, "Quality profile already exists: %s", name);
    return doCreate(dbSession, organization, name, false, false);
  }

  /**
   * Create the quality profile in DB with the specified name.
   *
   * A DB error will be thrown if the quality profile already exists.
   */
  public QProfileDto createBuiltIn(DbSession dbSession, OrganizationDto organization, QProfileName name, boolean isDefault) {
    return doCreate(dbSession, requireNonNull(organization), name, isDefault, true);
  }

  private QProfileDto doCreate(DbSession dbSession, OrganizationDto organization, QProfileName name, boolean isDefault, boolean isBuiltIn) {
    if (StringUtils.isEmpty(name.getName())) {
      throw BadRequestException.create("quality_profiles.profile_name_cant_be_blank");
    }
    Date now = new Date(system2.now());
    QProfileDto dto = QProfileDto.createFor(uuidFactory.create())
      .setName(name.getName())
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(name.getLanguage())
      .setIsBuiltIn(isBuiltIn)
      .setRulesUpdatedAtAsDate(now);
    db.qualityProfileDao().insert(dbSession, dto);
    if (isDefault) {
      db.defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(dto));
    }
    return dto;
  }

  // ------------- DELETION

  /**
   * Deletes the profiles with specified keys from database and Elasticsearch.
   * All related information are deleted. The profiles marked as "default"
   * are deleted too. Deleting a parent profile does not delete descendants
   * if their keys are not listed.
   */
  public void deleteByKeys(DbSession dbSession, Collection<String> profileUuids) {
    if (!profileUuids.isEmpty()) {
      db.qualityProfileDao().deleteProjectAssociationsByProfileUuids(dbSession, profileUuids);
      db.activeRuleDao().deleteParametersByProfileKeys(dbSession, profileUuids);
      db.activeRuleDao().deleteByProfileKeys(dbSession, profileUuids);
      db.qProfileChangeDao().deleteByProfileKeys(dbSession, profileUuids);
      db.defaultQProfileDao().deleteByQProfileUuids(dbSession, profileUuids);
      db.qualityProfileDao().deleteByUuids(dbSession, profileUuids);
      dbSession.commit();
      activeRuleIndexer.deleteByProfileKeys(profileUuids);
    }
  }

}
