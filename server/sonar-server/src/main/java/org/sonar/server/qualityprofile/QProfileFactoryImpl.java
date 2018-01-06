/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

public class QProfileFactoryImpl implements QProfileFactory {

  private final DbClient db;
  private final UuidFactory uuidFactory;
  private final System2 system2;
  private final ActiveRuleIndexer activeRuleIndexer;

  public QProfileFactoryImpl(DbClient db, UuidFactory uuidFactory, System2 system2, ActiveRuleIndexer activeRuleIndexer) {
    this.db = db;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  private static OrganizationDto requireNonNull(@Nullable OrganizationDto organization) {
    Objects.requireNonNull(organization, "Organization is required, when creating a quality profile.");
    return organization;
  }

  @Override
  public QProfileDto getOrCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    requireNonNull(organization);
    QProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, name.getName(), name.getLanguage());
    if (profile == null) {
      profile = doCreate(dbSession, organization, name, false, false);
    } else {
      checkArgument(!profile.isBuiltIn(), "Operation forbidden for built-in Quality Profile '%s' with language '%s'", profile.getName(), profile.getLanguage());
    }

    return profile;
  }

  @Override
  public QProfileDto checkAndCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    requireNonNull(organization);
    QProfileDto dto = db.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, name.getName(), name.getLanguage());
    checkRequest(dto == null, "Quality profile already exists: %s", name);
    return doCreate(dbSession, organization, name, false, false);
  }

  private QProfileDto doCreate(DbSession dbSession, OrganizationDto organization, QProfileName name, boolean isDefault, boolean isBuiltIn) {
    if (StringUtils.isEmpty(name.getName())) {
      throw BadRequestException.create("quality_profiles.profile_name_cant_be_blank");
    }
    Date now = new Date(system2.now());
    QProfileDto dto = new QProfileDto()
      .setKee(uuidFactory.create())
      .setRulesProfileUuid(uuidFactory.create())
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
  @Override
  public void delete(DbSession dbSession, Collection<QProfileDto> profiles) {
    if (profiles.isEmpty()) {
      return;
    }

    Set<String> uuids = new HashSet<>();
    List<QProfileDto> customProfiles = new ArrayList<>();
    Set<String> rulesProfileUuidsOfCustomProfiles = new HashSet<>();
    profiles.forEach(p -> {
      uuids.add(p.getKee());
      if (!p.isBuiltIn()) {
        customProfiles.add(p);
        rulesProfileUuidsOfCustomProfiles.add(p.getRulesProfileUuid());
      }
    });

    // tables org_qprofiles, default_qprofiles and project_qprofiles
    // are deleted whatever custom or built-in
    db.qualityProfileDao().deleteProjectAssociationsByProfileUuids(dbSession, uuids);
    db.defaultQProfileDao().deleteByQProfileUuids(dbSession, uuids);
    db.qualityProfileDao().deleteOrgQProfilesByUuids(dbSession, uuids);

    // Permissions are only available on custom profiles
    db.qProfileEditUsersDao().deleteByQProfiles(dbSession, customProfiles);
    db.qProfileEditGroupsDao().deleteByQProfiles(dbSession, customProfiles);

    // tables related to rules_profiles and active_rules are deleted
    // only for custom profiles. Built-in profiles are never
    // deleted from table rules_profiles.
    if (!rulesProfileUuidsOfCustomProfiles.isEmpty()) {
      db.activeRuleDao().deleteParametersByRuleProfileUuids(dbSession, rulesProfileUuidsOfCustomProfiles);
      db.activeRuleDao().deleteByRuleProfileUuids(dbSession, rulesProfileUuidsOfCustomProfiles);
      db.qProfileChangeDao().deleteByRulesProfileUuids(dbSession, rulesProfileUuidsOfCustomProfiles);
      db.qualityProfileDao().deleteRulesProfilesByUuids(dbSession, rulesProfileUuidsOfCustomProfiles);
      activeRuleIndexer.commitDeletionOfProfiles(dbSession, customProfiles);
    } else {
      dbSession.commit();
    }
  }
}
