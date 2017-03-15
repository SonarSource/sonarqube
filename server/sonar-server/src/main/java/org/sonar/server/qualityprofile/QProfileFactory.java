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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.util.Slug;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.ws.QProfileReference;

import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Create, delete and set as default profile.
 */
public class QProfileFactory {

  private final DbClient db;

  public QProfileFactory(DbClient db) {
    this.db = db;
  }

  // ------------- CREATION

  QualityProfileDto getOrCreate(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    Objects.requireNonNull(organization, "Organization is required, when creating a quality profile.");
    QualityProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(organization, name.getName(), name.getLanguage(), dbSession);
    if (profile == null) {
      profile = doCreate(dbSession, organization, name);
    }
    return profile;
  }

  public QualityProfileDto create(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    Objects.requireNonNull(organization, "Organization is required, when creating a quality profile.");
    QualityProfileDto dto = db.qualityProfileDao().selectByNameAndLanguage(organization, name.getName(), name.getLanguage(), dbSession);
    checkRequest(dto == null, "Quality profile already exists: %s", name);
    return doCreate(dbSession, organization, name);
  }

  private QualityProfileDto doCreate(DbSession dbSession, OrganizationDto organization, QProfileName name) {
    if (StringUtils.isEmpty(name.getName())) {
      throw BadRequestException.create("quality_profiles.profile_name_cant_be_blank");
    }
    Date now = new Date();
    for (int i = 0; i < 20; i++) {
      String key = Slug.slugify(String.format("%s %s %s", name.getLanguage(), name.getName(), RandomStringUtils.randomNumeric(5)));
      QualityProfileDto dto = QualityProfileDto.createFor(key)
        .setName(name.getName())
        .setOrganizationUuid(organization.getUuid())
        .setLanguage(name.getLanguage())
        .setRulesUpdatedAtAsDate(now);
      if (db.qualityProfileDao().selectByKey(dbSession, dto.getKey()) == null) {
        db.qualityProfileDao().insert(dbSession, dto);
        return dto;
      }
    }

    throw new IllegalStateException("Failed to create an unique quality profile for " + name);
  }

  // ------------- DELETION

  /**
   * Session is NOT committed. Profiles marked as "default" for a language can't be deleted,
   * except if the parameter <code>force</code> is true.
   */
  public List<ActiveRuleChange> delete(DbSession session, String key, boolean force) {
    QualityProfileDto profile = db.qualityProfileDao().selectOrFailByKey(session, key);
    List<QualityProfileDto> descendants = db.qualityProfileDao().selectDescendants(session, key);
    if (!force) {
      checkNotDefault(profile);
      for (QualityProfileDto descendant : descendants) {
        checkNotDefault(descendant);
      }
    }
    // delete bottom-up
    List<ActiveRuleChange> changes = new ArrayList<>();
    for (QualityProfileDto descendant : Lists.reverse(descendants)) {
      changes.addAll(doDelete(session, descendant));
    }
    changes.addAll(doDelete(session, profile));
    return changes;
  }

  private List<ActiveRuleChange> doDelete(DbSession session, QualityProfileDto profile) {
    db.qualityProfileDao().deleteAllProjectProfileAssociation(profile.getKey(), session);
    List<ActiveRuleChange> changes = new ArrayList<>();
    for (ActiveRuleDto activeRule : db.activeRuleDao().selectByProfileKey(session, profile.getKey())) {
      db.activeRuleDao().delete(session, activeRule.getKey());
      changes.add(ActiveRuleChange.createFor(DEACTIVATED, activeRule.getKey()));
    }
    db.qualityProfileDao().delete(session, profile.getId());
    return changes;
  }

  // ------------- DEFAULT PROFILE

  public List<QualityProfileDto> getDefaults(DbSession session, OrganizationDto organization, Collection<String> languageKeys) {
    return db.qualityProfileDao().selectDefaultProfiles(session, organization, languageKeys);
  }

  /**
   * @deprecated replaced by {@link org.sonar.server.qualityprofile.ws.QProfileWsSupport#getProfile(DbSession, QProfileReference)}
   */
  @Deprecated
  public QualityProfileDto find(DbSession dbSession, QProfileRef ref) {
    if (ref.hasKey()) {
      return findByKey(dbSession, ref.getKey());
    }
    return findByName(dbSession, ref.getLanguage(), ref.getName());
  }

  private QualityProfileDto findByKey(DbSession dbSession, String profileKey) {
    QualityProfileDto profile;
    profile = db.qualityProfileDao().selectByKey(dbSession, profileKey);
    return checkFound(profile, "Unable to find a profile for with key '%s'", profileKey);
  }

  private QualityProfileDto findByName(DbSession dbSession, String language, String profileName) {
    QualityProfileDto profile;
    profile = db.qualityProfileDao().selectByNameAndLanguage(profileName, language, dbSession);
    return checkFound(profile, "Unable to find a profile for language '%s' with name '%s'", language, profileName);
  }

  @CheckForNull
  public QualityProfileDto getByProjectAndLanguage(DbSession session, String projectKey, String language) {
    return db.qualityProfileDao().selectByProjectAndLanguage(session, projectKey, language);
  }

  public List<QualityProfileDto> getByProjectAndLanguages(DbSession session, OrganizationDto organization, String projectKey, Set<String> languageKeys) {
    return db.qualityProfileDao().selectByProjectAndLanguages(session, organization, projectKey, languageKeys);
  }

  public List<QualityProfileDto> getByNameAndLanguages(DbSession session, OrganizationDto organization, String name, Collection<String> languages) {
    return db.qualityProfileDao().selectByNameAndLanguages(organization, name, languages, session);
  }

  private static void checkNotDefault(QualityProfileDto p) {
    if (p.isDefault()) {
      throw BadRequestException.create("The profile marked as default can not be deleted: " + p.getKey());
    }
  }
}
