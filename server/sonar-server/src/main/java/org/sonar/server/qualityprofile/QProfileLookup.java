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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class QProfileLookup {

  private final DbClient db;

  public QProfileLookup(DbClient db) {
    this.db = db;
  }

  public List<QProfile> allProfiles() {
    return toQProfiles(db.qualityProfileDao().findAll());
  }

  public List<QProfile> profiles(String language) {
    return toQProfiles(db.qualityProfileDao().findByLanguage(language));
  }

  @CheckForNull
  public QProfile profile(int id) {
    DbSession session = db.openSession(false);
    try {
      return profile(id, session);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public QProfile profile(int id, DbSession session) {
    QualityProfileDto dto = findQualityProfile(id, session);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

  public QProfile profile(String name, String language, DbSession session) {
    QualityProfileDto dto = findQualityProfile(name, language, session);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

  @CheckForNull
  public QProfile profile(String name, String language) {
    DbSession session = db.openSession(false);
    try {
      return profile(name, language, session);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public QProfile parent(QProfile profile) {
    DbSession session = db.openSession(false);
    try {
      String parent = profile.parent();
      if (parent != null) {
        QualityProfileDto parentDto = findQualityProfile(parent, profile.language(), session);
        if (parentDto != null) {
          return QProfile.from(parentDto);
        }
      }
      return null;
    } finally {
      session.close();
    }
  }

  public List<QProfile> children(QProfile profile) {
    DbSession session = db.openSession(false);
    try {
      return children(profile, session);
    } finally {
      session.close();
    }
  }

  public List<QProfile> children(QProfile profile, DbSession session) {
    return toQProfiles(db.qualityProfileDao().findChildren(session, profile.key()));
  }

  public List<QProfile> ancestors(QualityProfileDto profile, DbSession session) {
    List<QProfile> ancestors = newArrayList();
    incrementAncestors(QProfile.from(profile), ancestors, session);
    return ancestors;
  }

  public List<QProfile> ancestors(QProfile profile) {
    List<QProfile> ancestors = newArrayList();
    DbSession session = db.openSession(false);
    try {
      incrementAncestors(profile, ancestors, session);
    } finally {
      session.close();
    }
    return ancestors;
  }

  private void incrementAncestors(QProfile profile, List<QProfile> ancestors, DbSession session) {
    if (profile.parent() != null) {
      QualityProfileDto parentDto = db.qualityProfileDao().getParentById(profile.id(), session);
      if (parentDto == null) {
        throw new IllegalStateException("Cannot find parent of profile : " + profile.id());
      }
      QProfile parent = QProfile.from(parentDto);
      ancestors.add(parent);
      incrementAncestors(parent, ancestors, session);
    }
  }

  private List<QProfile> toQProfiles(List<QualityProfileDto> dtos) {
    return newArrayList(Iterables.transform(dtos, new Function<QualityProfileDto, QProfile>() {
      @Override
      public QProfile apply(QualityProfileDto input) {
        return QProfile.from(input);
      }
    }));
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(int id, DbSession session) {
    return db.qualityProfileDao().getById(id, session);
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(String name, String language, DbSession session) {
    return db.qualityProfileDao().getByNameAndLanguage(name, language, session);
  }

}
