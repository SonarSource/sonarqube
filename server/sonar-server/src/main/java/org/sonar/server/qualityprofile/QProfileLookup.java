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

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.core.util.stream.Collectors.toList;

import java.util.List;

import javax.annotation.CheckForNull;

import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;

@ServerSide
public class QProfileLookup {

  private final DbClient db;

  public QProfileLookup(DbClient db) {
    this.db = db;
  }

  public List<QProfile> allProfiles(DbSession dbSession, OrganizationDto organization) {
    return toQProfiles(db.qualityProfileDao().selectAll(dbSession, organization), organization);
  }

  public List<QProfile> profiles(DbSession dbSession, String language, OrganizationDto organization) {
    return toQProfiles(db.qualityProfileDao().selectByLanguage(dbSession, language), organization);
  }

  @CheckForNull
  public QProfile profile(String name, String language) {
    try (DbSession dbSession = db.openSession(false)) {
      QualityProfileDto dto = findQualityProfile(name, language, dbSession);
      if (dto != null) {
        return QProfile.from(dto, null);
      }
      return null;
    }
  }

  public List<QProfile> ancestors(QualityProfileDto profile, DbSession session) {
    List<QProfile> ancestors = newArrayList();
    incrementAncestors(QProfile.from(profile, null), ancestors, session);
    return ancestors;
  }

  private void incrementAncestors(QProfile profile, List<QProfile> ancestors, DbSession session) {
    if (profile.parent() != null) {
      QualityProfileDto parentDto = db.qualityProfileDao().selectParentById(session, profile.id());
      if (parentDto == null) {
        throw new IllegalStateException("Cannot find parent of profile : " + profile.id());
      }
      QProfile parent = QProfile.from(parentDto, null);
      ancestors.add(parent);
      incrementAncestors(parent, ancestors, session);
    }
  }

  private static List<QProfile> toQProfiles(List<QualityProfileDto> dtos, OrganizationDto organization) {
    return dtos.stream().map(dto -> QProfile.from(dto, organization)).collect(toList(dtos.size()));
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(String name, String language, DbSession session) {
    return db.qualityProfileDao().selectByNameAndLanguage(name, language, session);
  }
}
