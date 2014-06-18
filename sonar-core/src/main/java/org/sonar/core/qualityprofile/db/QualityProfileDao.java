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

package org.sonar.core.qualityprofile.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.List;

public class QualityProfileDao implements ServerComponent, DaoComponent {

  private final MyBatis mybatis;

  public QualityProfileDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  @CheckForNull
  public QualityProfileDto getByKey(DbSession session, String key) {
    return session.getMapper(QualityProfileMapper.class).selectByKey(key);
  }

  public QualityProfileDto getNonNullByKey(DbSession session, String key) {
    QualityProfileDto dto = getByKey(session, key);
    if (dto == null) {
      throw new IllegalArgumentException("Quality profile not found: " + key);
    }
    return dto;
  }

  public List<QualityProfileDto> findAll(DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectAll();
  }

  public void insert(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = session.getMapper(QualityProfileMapper.class);
    doInsert(mapper, profile);
    for (QualityProfileDto other : otherProfiles) {
      doInsert(mapper, other);
    }
  }

  private void doInsert(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkArgument(profile.getId() == null, "Quality profile is already persisted (got id %d)", profile.getId());
    mapper.insert(profile);
  }

  /**
   * @deprecated use {@link #insert(org.sonar.core.persistence.DbSession, QualityProfileDto, QualityProfileDto...)}
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
    QualityProfileMapper mapper = session.getMapper(QualityProfileMapper.class);
    doUpdate(mapper, profile);
    for (QualityProfileDto otherProfile : otherProfiles) {
      doUpdate(mapper, otherProfile);
    }
  }

  private void doUpdate(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkArgument(profile.getId() != null, "Quality profile is not persisted");
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
    QualityProfileMapper mapper = session.getMapper(QualityProfileMapper.class);
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
    session.getMapper(QualityProfileMapper.class).delete(id);
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
   * @deprecated Replaced by
   *    {@link #findAll(DbSession)}
   */
  @Deprecated
  public List<QualityProfileDto> findAll() {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).selectAll();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto getDefaultProfile(String language, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectDefaultProfile(language, String.format("sonar.profile.%s", language));
  }

  @CheckForNull
  public QualityProfileDto getDefaultProfile(String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return getDefaultProfile(language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto getByProjectAndLanguage(long projectId, String language, String key) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).selectByProjectAndLanguage(projectId, language, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> findByLanguage(String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).selectByLanguage(language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @deprecated Replaced by
   *    {@link #getByKey(org.sonar.core.persistence.DbSession, String)}
   */
  @Deprecated
  @CheckForNull
  public QualityProfileDto getById(int id, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectById(id);
  }

  /**
   * @deprecated Replaced by
   *    {@link #getByKey(org.sonar.core.persistence.DbSession, String)}
   */
  @Deprecated
  @CheckForNull
  public QualityProfileDto getById(int id) {
    DbSession session = mybatis.openSession(false);
    try {
      return getById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto getParent(String childKey, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectParent(childKey);
  }

  @CheckForNull
  public QualityProfileDto getParent(String childKey) {
    DbSession session = mybatis.openSession(false);
    try {
      return getParent(childKey, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto getParentById(int childId, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectParentById(childId);
  }

  @CheckForNull
  public QualityProfileDto getParentById(int childId) {
    DbSession session = mybatis.openSession(false);
    try {
      return getParentById(childId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> findChildren(DbSession session, String key) {
    return session.getMapper(QualityProfileMapper.class).selectChildren(key);
  }

  /**
   * All descendants, in the top-down order.
   */
  public List<QualityProfileDto> findDescendants(DbSession session, String key) {
    List<QualityProfileDto> descendants = Lists.newArrayList();
    for (QualityProfileDto child : findChildren(session, key)) {
      descendants.add(child);
      descendants.addAll(findDescendants(session, child.getKey()));
    }
    return descendants;
  }

  @CheckForNull
  public QualityProfileDto getByNameAndLanguage(String name, String language, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectByNameAndLanguage(name, language);
  }

  /**
   * @deprecated Replaced by
   *    {@link #getByNameAndLanguage(String, String, org.sonar.core.persistence.DbSession)}
   */
  @Deprecated
  public QualityProfileDto getByNameAndLanguage(String name, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return getByNameAndLanguage(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectProjects(propertyKey, propertyValue, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectProjects(propertyKey, propertyValue);
  }

  public int countProjects(String propertyKey, String propertyValue) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).countProjects(propertyKey, propertyValue);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
