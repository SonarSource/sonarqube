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
package org.sonar.db.property;

import com.google.common.base.Strings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class PropertiesDao implements Dao {

  private static final String NOTIFICATION_PREFIX = "notification.";
  private MyBatis mybatis;

  public PropertiesDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * Returns the logins of users who have subscribed to the given notification dispatcher with the given notification channel.
   * If a resource ID is passed, the search is made on users who have specifically subscribed for the given resource.
   *
   * @return the list of logins (maybe be empty - obviously)
   */
  public List<String> selectUsersForNotification(String notificationDispatcherKey, String notificationChannelKey, @Nullable String projectUuid) {
    try (DbSession session = mybatis.openSession(false)) {
      return getMapper(session).findUsersForNotification(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, projectUuid);
    }
  }

  public List<String> selectNotificationSubscribers(String notificationDispatcherKey, String notificationChannelKey, @Nullable String componentKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return getMapper(session).findNotificationSubscribers(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, componentKey);
    }
  }

  public boolean hasProjectNotificationSubscribersForDispatchers(String projectUuid, Collection<String> dispatcherKeys) {
    try (DbSession session = mybatis.openSession(false);
      Connection connection = session.getConnection();
      PreparedStatement pstmt = createStatement(projectUuid, dispatcherKeys, connection);
      ResultSet rs = pstmt.executeQuery()) {
      return rs.next() && rs.getInt(1) > 0;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute SQL for hasProjectNotificationSubscribersForDispatchers", e);
    }
  }

  private static PreparedStatement createStatement(String projectUuid, Collection<String> dispatcherKeys, Connection connection) throws SQLException {
    String sql = "SELECT count(1) FROM properties pp " +
      "left outer join projects pj on pp.resource_id = pj.id " +
      "where pp.user_id is not null and (pp.resource_id is null or pj.uuid=?) " +
      "and (" + DatabaseUtils.repeatCondition("pp.prop_key like ?", dispatcherKeys.size(), "or") + ")";
    PreparedStatement res = connection.prepareStatement(sql);
    res.setString(1, projectUuid);
    int index = 2;
    for (String dispatcherKey : dispatcherKeys) {
      res.setString(index, "notification." + dispatcherKey + ".%");
      index++;
    }
    return res;
  }

  public List<PropertyDto> selectGlobalProperties() {
    try (DbSession session = mybatis.openSession(false)) {
      return selectGlobalProperties(session);
    }
  }

  public List<PropertyDto> selectGlobalProperties(DbSession session) {
    return getMapper(session).selectGlobalProperties();
  }

  @CheckForNull
  public PropertyDto selectGlobalProperty(DbSession session, String propertyKey) {
    return getMapper(session).selectByKey(new PropertyDto().setKey(propertyKey));
  }

  @CheckForNull
  public PropertyDto selectGlobalProperty(String propertyKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectGlobalProperty(session, propertyKey);
    }
  }

  public List<PropertyDto> selectProjectProperties(DbSession session, String projectKey) {
    return getMapper(session).selectProjectProperties(projectKey);
  }

  public List<PropertyDto> selectProjectProperties(String resourceKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectProjectProperties(session, resourceKey);
    }
  }

  public List<PropertyDto> selectEnabledDescendantModuleProperties(String moduleUuid, DbSession session) {
    return getMapper(session).selectDescendantModuleProperties(moduleUuid, Scopes.PROJECT, true);
  }

  @CheckForNull
  public PropertyDto selectProjectProperty(long resourceId, String propertyKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectProjectProperty(session, resourceId, propertyKey);
    }
  }

  @CheckForNull
  public PropertyDto selectProjectProperty(DbSession dbSession, long resourceId, String propertyKey) {
    return getMapper(dbSession).selectByKey(new PropertyDto().setKey(propertyKey).setResourceId(resourceId));
  }

  public List<PropertyDto> selectByQuery(PropertyQuery query, DbSession session) {
    return getMapper(session).selectByQuery(query);
  }

  public List<PropertyDto> selectGlobalPropertiesByKeys(DbSession session, Set<String> keys) {
    return selectByKeys(session, keys, null);
  }

  public List<PropertyDto> selectPropertiesByKeysAndComponentId(DbSession session, Set<String> keys, long componentId) {
    return selectByKeys(session, keys, componentId);
  }

  public List<PropertyDto> selectPropertiesByKeysAndComponentIds(DbSession session, Set<String> keys, Set<Long> componentIds) {
    return executeLargeInputs(keys, partitionKeys -> executeLargeInputs(componentIds,
      partitionComponentIds -> getMapper(session).selectByKeysAndComponentIds(partitionKeys, partitionComponentIds)));
  }

  private List<PropertyDto> selectByKeys(DbSession session, Set<String> keys, @Nullable Long componentId) {
    return executeLargeInputs(keys, partitionKeys -> getMapper(session).selectByKeys(partitionKeys, componentId));
  }

  public void insertProperty(DbSession session, PropertyDto property) {
    PropertiesMapper mapper = getMapper(session);
    PropertyDto persistedProperty = mapper.selectByKey(property);
    if (persistedProperty != null && !StringUtils.equals(persistedProperty.getValue(), property.getValue())) {
      persistedProperty.setValue(property.getValue());
      mapper.update(persistedProperty);
    } else if (persistedProperty == null) {
      mapper.insert(property);
    }
  }

  public void insertProperty(PropertyDto property) {
    try (DbSession session = mybatis.openSession(false)) {
      insertProperty(session, property);
      session.commit();
    }
  }

  public void deleteById(DbSession dbSession, long id) {
    getMapper(dbSession).deleteById(id);
  }

  public void deleteProjectProperty(String key, Long projectId) {
    try (DbSession session = mybatis.openSession(false)) {
      deleteProjectProperty(key, projectId, session);
      session.commit();
    }
  }

  public void deleteProjectProperty(String key, Long projectId, DbSession session) {
    getMapper(session).deleteProjectProperty(key, projectId);
  }

  public void deleteProjectProperties(String key, String value, DbSession session) {
    getMapper(session).deleteProjectProperties(key, value);
  }

  public void deleteProjectProperties(String key, String value) {
    try (DbSession session = mybatis.openSession(false)) {
      deleteProjectProperties(key, value, session);
      session.commit();
    }
  }

  public void deleteGlobalProperty(String key, DbSession session) {
    getMapper(session).deleteGlobalProperty(key);
  }

  public void deleteGlobalProperty(String key) {
    try (DbSession session = mybatis.openSession(false)) {
      deleteGlobalProperty(key, session);
      session.commit();
    }
  }

  public void insertGlobalProperties(Map<String, String> properties) {
    try (DbSession session = mybatis.openSession(false)) {
      PropertiesMapper mapper = getMapper(session);
      properties.entrySet().forEach(entry -> delete(mapper, entry));
      properties.entrySet().forEach(entry -> insert(mapper, entry));
      session.commit();
    }
  }

  private static void delete(PropertiesMapper mapper, Map.Entry<String, String> entry) {
    mapper.deleteGlobalProperty(entry.getKey());
  }

  private static void insert(PropertiesMapper mapper, Map.Entry<String, String> entry) {
    mapper.insert(new PropertyDto().setKey(entry.getKey()).setValue(entry.getValue()));
  }

  public void renamePropertyKey(String oldKey, String newKey) {
    checkArgument(!Strings.isNullOrEmpty(oldKey), "Old property key must not be empty");
    checkArgument(!Strings.isNullOrEmpty(newKey), "New property key must not be empty");

    if (!newKey.equals(oldKey)) {
      try (DbSession session = mybatis.openSession(false)) {
        getMapper(session).renamePropertyKey(oldKey, newKey);
        session.commit();
      }
    }
  }

  private static PropertiesMapper getMapper(DbSession session) {
    return session.getMapper(PropertiesMapper.class);
  }

}
