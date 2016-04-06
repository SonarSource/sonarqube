/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.qualityprofile;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;

public class QualityProfileDao implements Dao {

  private final MyBatis mybatis;
  private final System2 system;

  public QualityProfileDao(MyBatis mybatis, System2 system) {
    this.mybatis = mybatis;
    this.system = system;
  }

  @CheckForNull
  public QualityProfileDto selectByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public QualityProfileDto selectOrFailByKey(DbSession session, String key) {
    QualityProfileDto dto = selectByKey(session, key);
    if (dto == null) {
      throw new RowNotFoundException("Quality profile not found: " + key);
    }
    return dto;
  }

  public List<QualityProfileDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public void insert(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    doInsert(mapper, profile);
    for (QualityProfileDto other : otherProfiles) {
      doInsert(mapper, other);
    }
  }

  private void doInsert(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkArgument(profile.getId() == null, "Quality profile is already persisted (got id %d)", profile.getId());
    Date now = new Date(system.now());
    profile.setCreatedAt(now);
    profile.setUpdatedAt(now);
    mapper.insert(profile);
  }

  /**
   * @deprecated use {@link #insert(DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void insert(QualityProfileDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    doUpdate(mapper, profile);
    for (QualityProfileDto otherProfile : otherProfiles) {
      doUpdate(mapper, otherProfile);
    }
  }

  private void doUpdate(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkArgument(profile.getId() != null, "Quality profile is not persisted");
    profile.setUpdatedAt(new Date(system.now()));
    mapper.update(profile);
  }

  /**
   * @deprecated use {@link #update(DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void update(QualityProfileDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      update(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    doDelete(mapper, profile);
    for (QualityProfileDto otherProfile : otherProfiles) {
      doDelete(mapper, otherProfile);
    }
  }

  private void doDelete(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkNotNull(profile.getId(), "Quality profile is not persisted");
    mapper.delete(profile.getId());
  }

  /**
   * @deprecated use {@link #delete(DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void delete(int id, DbSession session) {
    mapper(session).delete(id);
  }

  /**
   * @deprecated use {@link #delete(DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void delete(int id) {
    DbSession session = mybatis.openSession(false);
    try {
      delete(id, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @deprecated Replaced by {@link #selectAll(DbSession)}
   */
  @Deprecated
  public List<QualityProfileDto> selectAll() {
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectAll();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public List<QualityProfileDto> selectDefaultProfiles(final DbSession session, Collection<String> languageKeys) {
    return DatabaseUtils.executeLargeInputs(languageKeys, new Function<List<String>, List<QualityProfileDto>>() {
      @Override
      @Nonnull
      public List<QualityProfileDto> apply(@Nonnull List<String> input) {
        return mapper(session).selectDefaultProfiles(input);
      }
    });
  }

  @CheckForNull
  public QualityProfileDto selectDefaultProfile(final DbSession session, String language) {
    return mapper(session).selectDefaultProfile(language);
  }

  @CheckForNull
  public QualityProfileDto selectDefaultProfile(String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectDefaultProfile(session, language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectByProjectAndLanguage(long projectId, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectByProjectIdAndLanguage(projectId, language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectByProjectAndLanguage(DbSession session, String projectKey, String language) {
    return mapper(session).selectByProjectAndLanguage(projectKey, language);
  }

  public List<QualityProfileDto> selectByProjectAndLanguages(final DbSession session, final String projectKey, Collection<String> languageKeys) {
    return DatabaseUtils.executeLargeInputs(languageKeys, new Function<List<String>, List<QualityProfileDto>>() {
      @Override
      @Nonnull
      public List<QualityProfileDto> apply(@Nonnull List<String> input) {
        return mapper(session).selectByProjectAndLanguages(projectKey, input);
      }
    });
  }

  public List<QualityProfileDto> selectByLanguage(String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectByLanguage(language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectById(DbSession session, int id) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public QualityProfileDto selectById(int id) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectById(session, id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectParent(DbSession session, String childKey) {
    return mapper(session).selectParent(childKey);
  }

  @CheckForNull
  public QualityProfileDto selectParent(String childKey) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParent(session, childKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectParentById(DbSession session, int childId) {
    return mapper(session).selectParentById(childId);
  }

  @CheckForNull
  public QualityProfileDto selectParentById(int childId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParentById(session, childId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectChildren(DbSession session, String key) {
    return mapper(session).selectChildren(key);
  }

  /**
   * All descendants, in the top-down order.
   */
  public List<QualityProfileDto> selectDescendants(DbSession session, String key) {
    List<QualityProfileDto> descendants = Lists.newArrayList();
    for (QualityProfileDto child : selectChildren(session, key)) {
      descendants.add(child);
      descendants.addAll(selectDescendants(session, child.getKey()));
    }
    return descendants;
  }

  @CheckForNull
  public QualityProfileDto selectByNameAndLanguage(String name, String language, DbSession session) {
    return mapper(session).selectByNameAndLanguage(name, language);
  }

  public List<QualityProfileDto> selectByNameAndLanguages(final String name, Collection<String> languageKeys, final DbSession session) {
    return DatabaseUtils.executeLargeInputs(languageKeys, new Function<List<String>, List<QualityProfileDto>>() {
      @Override
      @Nonnull
      public List<QualityProfileDto> apply(@Nonnull List<String> input) {
        return mapper(session).selectByNameAndLanguages(name, input);
      }
    });
  }

  public List<ComponentDto> selectProjects(String profileName, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectProjects(profileName, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String profileName, String language, DbSession session) {
    return mapper(session).selectProjects(profileName, language);
  }

  public int countProjects(String profileName, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).countProjects(profileName, language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Map<String, Long> countProjectsByProfileKey() {
    DbSession session = mybatis.openSession(false);
    try {
      Map<String, Long> countByKey = Maps.newHashMap();
      for (QualityProfileProjectCount count : mapper(session).countProjectsByProfile()) {
        countByKey.put(count.getProfileKey(), count.getProjectCount());
      }
      return countByKey;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insertProjectProfileAssociation(String projectUuid, String profileKey, DbSession session) {
    mapper(session).insertProjectProfileAssociation(projectUuid, profileKey);
  }

  public void deleteProjectProfileAssociation(String projectUuid, String profileKey, DbSession session) {
    mapper(session).deleteProjectProfileAssociation(projectUuid, profileKey);
  }

  public void updateProjectProfileAssociation(String projectUuid, String profileKey, DbSession session) {
    mapper(session).updateProjectProfileAssociation(projectUuid, profileKey);
  }

  public void deleteAllProjectProfileAssociation(String profileKey, DbSession session) {
    mapper(session).deleteAllProjectProfileAssociation(profileKey);
  }

  public List<ProjectQprofileAssociationDto> selectSelectedProjects(String profileKey, @Nullable String query, DbSession session) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectSelectedProjects(profileKey, nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectDeselectedProjects(String profileKey, @Nullable String query, DbSession session) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectDeselectedProjects(profileKey, nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectProjectAssociations(String profileKey, @Nullable String query, DbSession session) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectProjectAssociations(profileKey, nameQuery);
  }

  private String sqlQueryString(String query) {
    return query == null ? "%" : "%" + query.toUpperCase(Locale.ENGLISH) + "%";
  }

  private static QualityProfileMapper mapper(DbSession session) {
    return session.getMapper(QualityProfileMapper.class);
  }
}
