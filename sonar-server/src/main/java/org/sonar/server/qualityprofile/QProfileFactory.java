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
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;

public class QProfileFactory implements ServerComponent {

  private static final String PROFILE_PROPERTY_PREFIX = "sonar.profile.";

  private final DbClient db;
  private final PreviewCache previewCache;

  public QProfileFactory(DbClient db, PreviewCache previewCache) {
    this.db = db;
    this.previewCache = previewCache;
  }

  @CheckForNull
  QualityProfileKey getDefault(String language) {
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto profile = getDefault(dbSession, language);
      return profile != null ? profile.getKey() : null;
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  QualityProfileDto getDefault(DbSession session, String language) {
    return db.qualityProfileDao().selectDefaultProfile(language, PROFILE_PROPERTY_PREFIX + language, session);
  }


  void setDefault(QualityProfileKey key) {
    DbSession dbSession = db.openSession(false);
    try {
      setDefault(dbSession, key);
    } finally {
      dbSession.close();
    }
  }

  void setDefault(DbSession dbSession, QualityProfileKey key) {
    QualityProfileDto profile = db.qualityProfileDao().getNonNullByKey(dbSession, key);
    setDefault(profile);
    dbSession.commit();
  }

  private void setDefault(QualityProfileDto profile) {
    db.propertiesDao().setProperty(new PropertyDto()
      .setKey("sonar.profile." + profile.getLanguage())
      .setValue(profile.getName()));
  }

  void delete(QualityProfileKey key) {
    DbSession session = db.openSession(false);
    try {
      QualityProfileDto profile = db.qualityProfileDao().getNonNullByKey(session, key);
      List<QualityProfileDto> descendants = db.qualityProfileDao().findDescendants(session, key);
      QualityProfileDto defaultProfile = getDefault(session, profile.getLanguage());
      checkNotDefault(defaultProfile, profile);
      for (QualityProfileDto descendant : descendants) {
        checkNotDefault(defaultProfile, descendant);
      }
      // delete bottom-up
      for (QualityProfileDto descendant : Lists.reverse(descendants)) {
        doDelete(session, descendant);
      }
      doDelete(session, profile);
      previewCache.reportGlobalModification(session);
      session.commit();
    } finally {
      session.close();
    }
  }

  private void checkNotDefault(@Nullable QualityProfileDto defaultProfile, QualityProfileDto p) {
    if (defaultProfile != null && defaultProfile.getKey().equals(p.getKey())) {
      throw new BadRequestException("The profile marked as default can not be deleted: " + p.getKey());
    }
  }

  private void doDelete(DbSession session, QualityProfileDto profile) {
    db.activeRuleDao().deleteByProfileKey(session, profile.getKey());
    db.qualityProfileDao().delete(session, profile);
    db.propertiesDao().deleteProjectProperties(PROFILE_PROPERTY_PREFIX + profile.getLanguage(), profile.getName(), session);
    // TODO delete changelog
  }
}
