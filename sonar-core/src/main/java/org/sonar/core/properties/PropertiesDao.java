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

package org.sonar.core.properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Scopes;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@BatchSide
@ServerSide
public class PropertiesDao implements DaoComponent {

  private static final String NOTIFICATION_PREFIX = "notification.";
  private MyBatis mybatis;

  public PropertiesDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * Returns the logins of users who have subscribed to the given notification dispatcher with the given notification channel.
   * If a resource ID is passed, the search is made on users who have specifically subscribed for the given resource.
   *
   * @param notificationDispatcherKey the key of the notification dispatcher
   * @param notificationChannelKey    the key of the notification channel
   * @param resourceId                the resource id
   * @return the list of logins (maybe be empty - obviously)
   */
  public List<String> findUsersForNotification(String notificationDispatcherKey, String notificationChannelKey, @Nullable Long resourceId) {
    SqlSession session = mybatis.openSession(false);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.findUsersForNotification(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, resourceId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<String> findNotificationSubscribers(String notificationDispatcherKey, String notificationChannelKey, @Nullable String componentKey) {
    SqlSession session = mybatis.openSession(false);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.findNotificationSubscribers(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, componentKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean hasProjectNotificationSubscribersForDispatchers(String projectUuid, Collection<String> dispatcherKeys) {
    DbSession session = mybatis.openSession(false);
    Connection connection = session.getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String sql = "SELECT count(*) FROM properties pp " +
      "left outer join projects pj on pp.resource_id = pj.id " +
      "where pp.user_id is not null and (pp.resource_id is null or pj.uuid=?) " +
      "and (" + DaoUtils.repeatCondition("pp.prop_key like ?", dispatcherKeys.size(), "or") + ")";
    try {
      pstmt = connection.prepareStatement(sql);
      pstmt.setString(1, projectUuid);
      int index = 2;
      for (String dispatcherKey : dispatcherKeys) {
        pstmt.setString(index, "notification." + dispatcherKey + ".%");
        index++;
      }
      rs = pstmt.executeQuery();
      return rs.next() && rs.getInt(1) > 0;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute SQL request: " + sql, e);
    } finally {
      DbUtils.closeQuietly(connection, pstmt, rs);
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectGlobalProperties() {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectGlobalProperties(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectGlobalProperties(SqlSession session) {
    return session.getMapper(PropertiesMapper.class).selectGlobalProperties();
  }

  public PropertyDto selectGlobalProperty(String propertyKey) {
    SqlSession session = mybatis.openSession(false);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.selectByKey(new PropertyDto().setKey(propertyKey));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectProjectProperties(String resourceKey, SqlSession session) {
    return session.getMapper(PropertiesMapper.class).selectProjectProperties(resourceKey);
  }

  public List<PropertyDto> selectProjectProperties(long resourceId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectProjectProperties(resourceId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectProjectProperties(long resourceId, SqlSession session) {
    return session.getMapper(PropertiesMapper.class).selectProjectPropertiesByResourceId(resourceId);
  }

  public List<PropertyDto> selectProjectProperties(String resourceKey) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectProjectProperties(resourceKey, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectEnabledDescendantModuleProperties(String moduleUuid, SqlSession session) {
    return session.getMapper(PropertiesMapper.class).selectDescendantModuleProperties(moduleUuid, Scopes.PROJECT, true);
  }

  public PropertyDto selectProjectProperty(long resourceId, String propertyKey) {
    SqlSession session = mybatis.openSession(false);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.selectByKey(new PropertyDto().setKey(propertyKey).setResourceId(resourceId));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectByQuery(PropertyQuery query, DbSession session) {
    return session.getMapper(PropertiesMapper.class).selectByQuery(query);
  }

  public void setProperty(PropertyDto property, SqlSession session) {
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    PropertyDto persistedProperty = mapper.selectByKey(property);
    if (persistedProperty != null && !StringUtils.equals(persistedProperty.getValue(), property.getValue())) {
      persistedProperty.setValue(property.getValue());
      mapper.update(persistedProperty);
    } else if (persistedProperty == null) {
      mapper.insert(property);
    }
  }

  public void setProperty(PropertyDto property) {
    SqlSession session = mybatis.openSession(false);
    try {
      setProperty(property, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteProjectProperty(String key, Long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      deleteProjectProperty(key, projectId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteProjectProperty(String key, Long projectId, SqlSession session) {
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    mapper.deleteProjectProperty(key, projectId);
  }

  public void deleteProjectProperties(String key, String value, SqlSession session) {
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    mapper.deleteProjectProperties(key, value);
  }

  public void deleteProjectProperties(String key, String value) {
    SqlSession session = mybatis.openSession(false);
    try {
      deleteProjectProperties(key, value, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteGlobalProperties() {
    SqlSession session = mybatis.openSession(false);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      mapper.deleteGlobalProperties();
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteGlobalProperty(String key, SqlSession session) {
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    mapper.deleteGlobalProperty(key);
  }

  public void deleteGlobalProperty(String key) {
    SqlSession session = mybatis.openSession(false);
    try {
      deleteGlobalProperty(key, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteAllProperties(String key) {
    SqlSession session = mybatis.openSession(false);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      mapper.deleteAllProperties(key);
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void saveGlobalProperties(Map<String, String> properties) {
    DbSession session = mybatis.openSession(true);
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        mapper.deleteGlobalProperty(entry.getKey());
      }
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        mapper.insert(new PropertyDto().setKey(entry.getKey()).setValue(entry.getValue()));
      }
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void renamePropertyKey(String oldKey, String newKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(oldKey), "Old property key must not be empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(newKey), "New property key must not be empty");

    if (!newKey.equals(oldKey)) {
      SqlSession session = mybatis.openSession(false);
      PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
      try {
        mapper.renamePropertyKey(oldKey, newKey);
        session.commit();

      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }

  /**
   * Update all properties (global and projects ones) with a given key and value to a new value
   */
  public void updateProperties(String key, String oldValue, String newValue) {
    SqlSession session = mybatis.openSession(false);
    try {
      updateProperties(key, oldValue, newValue, session);
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateProperties(String key, String oldValue, String newValue, SqlSession session) {
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    mapper.updateProperties(key, oldValue, newValue);
  }

}
