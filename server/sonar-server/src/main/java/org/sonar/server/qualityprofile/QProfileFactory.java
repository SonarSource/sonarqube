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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.util.Slug;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.Verifications;

/**
 * Create, delete, rename and set as default profile.
 */
public class QProfileFactory {

  private final DbClient db;

  public QProfileFactory(DbClient db) {
    this.db = db;
  }

  // ------------- CREATION

  QualityProfileDto getOrCreate(DbSession dbSession, QProfileName name) {
    QualityProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(name.getName(), name.getLanguage(), dbSession);
    if (profile == null) {
      profile = doCreate(dbSession, name);
    }
    return profile;
  }

  public QualityProfileDto create(DbSession dbSession, QProfileName name) {
    QualityProfileDto dto = db.qualityProfileDao().selectByNameAndLanguage(name.getName(), name.getLanguage(), dbSession);
    if (dto != null) {
      throw new BadRequestException("Quality profile already exists: " + name);
    }
    return doCreate(dbSession, name);
  }

  private QualityProfileDto doCreate(DbSession dbSession, QProfileName name) {
    if (StringUtils.isEmpty(name.getName())) {
      throw new BadRequestException("quality_profiles.profile_name_cant_be_blank");
    }
    Date now = new Date();
    for (int i = 0; i < 20; i++) {
      String key = Slug.slugify(String.format("%s %s %s", name.getLanguage(), name.getName(), RandomStringUtils.randomNumeric(5)));
      QualityProfileDto dto = QualityProfileDto.createFor(key)
        .setName(name.getName())
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

  void delete(String key) {
    DbSession session = db.openSession(false);
    try {
      delete(session, key, false);
      session.commit();
    } finally {
      session.close();
    }
  }

  /**
   * Session is NOT committed. Profiles marked as "default" for a language can't be deleted,
   * except if the parameter <code>force</code> is true.
   */
  public void delete(DbSession session, String key, boolean force) {
    QualityProfileDto profile = db.qualityProfileDao().selectOrFailByKey(session, key);
    List<QualityProfileDto> descendants = db.qualityProfileDao().selectDescendants(session, key);
    if (!force) {
      checkNotDefault(profile);
      for (QualityProfileDto descendant : descendants) {
        checkNotDefault(descendant);
      }
    }
    // delete bottom-up
    for (QualityProfileDto descendant : Lists.reverse(descendants)) {
      doDelete(session, descendant);
    }
    doDelete(session, profile);
  }

  private void doDelete(DbSession session, QualityProfileDto profile) {
    db.qualityProfileDao().deleteAllProjectProfileAssociation(profile.getKey(), session);
    db.activeRuleDao().deleteByProfileKey(session, profile.getKey());
    db.qualityProfileDao().delete(session, profile);
  }

  // ------------- DEFAULT PROFILE

  @CheckForNull
  QualityProfileDto getDefault(String language) {
    DbSession dbSession = db.openSession(false);
    try {
      return getDefault(dbSession, language);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getDefault(DbSession session, String language) {
    return db.qualityProfileDao().selectDefaultProfile(session, language);
  }

  public void setDefault(String profileKey) {
    DbSession dbSession = db.openSession(false);
    try {
      setDefault(dbSession, profileKey);
    } finally {
      dbSession.close();
    }
  }

  void setDefault(DbSession dbSession, String profileKey) {
    Verifications.check(StringUtils.isNotBlank(profileKey), "Profile key must be set");
    QualityProfileDto profile = db.qualityProfileDao().selectByKey(dbSession, profileKey);
    if (profile == null) {
      throw new NotFoundException("Quality profile not found: " + profileKey);
    }
    setDefault(dbSession, profile);
    dbSession.commit();
  }

  private void setDefault(DbSession session, QualityProfileDto profile) {
    QualityProfileDto previousDefault = db.qualityProfileDao().selectDefaultProfile(session, profile.getLanguage());
    if (previousDefault != null) {
      db.qualityProfileDao().update(session, previousDefault.setDefault(false));
    }
    db.qualityProfileDao().update(session, profile.setDefault(true));
  }

  QualityProfileDto getByProjectAndLanguage(String projectKey, String language) {
    DbSession dbSession = db.openSession(false);
    try {
      return getByProjectAndLanguage(dbSession, projectKey, language);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByProjectAndLanguage(DbSession session, String projectKey, String language) {
    return db.qualityProfileDao().selectByProjectAndLanguage(session, projectKey, language);
  }

  QualityProfileDto getByNameAndLanguage(String name, String language) {
    DbSession dbSession = db.openSession(false);
    try {
      return getByNameAndLanguage(dbSession, name, language);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByNameAndLanguage(DbSession session, String name, String language) {
    return db.qualityProfileDao().selectByNameAndLanguage(name, language, session);
  }

  private void checkNotDefault(QualityProfileDto p) {
    if (p.isDefault()) {
      throw new BadRequestException("The profile marked as default can not be deleted: " + p.getKey());
    }
  }

  // ------------- RENAME

  public boolean rename(String key, String newName) {
    Verifications.check(StringUtils.isNotBlank(newName), "Name must be set");
    Verifications.check(newName.length() < 100, String.format("Name is too long (>%d characters)", 100));
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto profile = db.qualityProfileDao().selectByKey(dbSession, key);
      if (profile == null) {
        throw new NotFoundException("Quality profile not found: " + key);
      }
      if (!StringUtils.equals(newName, profile.getName())) {
        if (db.qualityProfileDao().selectByNameAndLanguage(newName, profile.getLanguage(), dbSession) != null) {
          throw new BadRequestException("Quality profile already exists: " + newName);
        }
        profile.setName(newName);
        db.qualityProfileDao().update(dbSession, profile);
        dbSession.commit();
        return true;
      }
      return false;
    } finally {
      dbSession.close();
    }
  }
}
