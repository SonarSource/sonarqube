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
package org.sonar.core.persistence.migration.v44;

import org.apache.ibatis.annotations.Param;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

public interface Migration44Mapper {

  // migration of measures "profile" and "profile_version"
  List<ProfileMeasure> selectProfileMeasures();

  @CheckForNull
  Integer selectProfileVersion(long snapshotId);

  @CheckForNull
  Date selectProfileVersionDate(@Param("profileId") int profileId, @Param("profileVersion") int profileVersion);

  void updateProfileMeasure(@Param("measureId") long measureId, @Param("json") String json);

  void deleteProfileMeasure(long profileMeasureId);

  @CheckForNull
  QProfileDto44 selectProfileById(int id);

  // creation of columns RULES_PROFILES.CREATED_AT and UPDATED_AT
  List<QProfileDto44> selectAllProfiles();

  @CheckForNull
  Date selectProfileCreatedAt(int profileId);

  @CheckForNull
  Date selectProfileUpdatedAt(int profileId);

  void updateProfileDates(@Param("profileId") int profileId,
    @Param("createdAt") Date createdAt, @Param("updatedAt") Date updatedAt,
    @Param("rulesUpdatedAt") String rulesUpdatedAt);

  // migrate changeLog to Activities
  List<ChangeLog> selectActiveRuleChange(@Nullable @Param("enabled") Boolean enabled);

  List<Long> selectMeasuresOnDeletedQualityProfiles();
}
