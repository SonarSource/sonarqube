/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.property;

import com.google.common.base.Strings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.MyBatis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsIntoSet;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class PropertiesDao implements Dao {

  private static final String NOTIFICATION_PREFIX = "notification.";
  private static final int VARCHAR_MAXSIZE = 4000;

  private final MyBatis mybatis;
  private final System2 system2;

  public PropertiesDao(MyBatis mybatis, System2 system2) {
    this.mybatis = mybatis;
    this.system2 = system2;
  }

  /**
   * Returns the logins of users who have subscribed to the given notification dispatcher with the given notification channel.
   * If a resource ID is passed, the search is made on users who have specifically subscribed for the given resource.
   *
   * Note that {@link  UserRole#USER} permission is not checked here, filter the results with
   * {@link org.sonar.db.permission.AuthorizationDao#keepAuthorizedLoginsOnProject}
   *
   * @return the list of Subscriber (maybe be empty - obviously)
   */
  public Set<Subscriber> findUsersForNotification(String notificationDispatcherKey, String notificationChannelKey, @Nullable String projectKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return getMapper(session).findUsersForNotification(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, projectKey);
    }
  }

  public Set<EmailSubscriberDto> findEmailSubscribersForNotification(DbSession dbSession, String notificationDispatcherKey, String notificationChannelKey,
    @Nullable String projectKey) {
    return getMapper(dbSession).findEmailRecipientsForNotification(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, projectKey, null);
  }

  public Set<EmailSubscriberDto> findEmailSubscribersForNotification(DbSession dbSession, String notificationDispatcherKey, String notificationChannelKey,
    @Nullable String projectKey, Set<String> logins) {
    if (logins.isEmpty()) {
      return Collections.emptySet();
    }

    return executeLargeInputsIntoSet(
      logins,
      loginsPartition -> {
        String notificationKey = NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey;
        return getMapper(dbSession).findEmailRecipientsForNotification(notificationKey, projectKey, loginsPartition);
      },
      partitionSize -> projectKey == null ? partitionSize : (partitionSize / 2));
  }

  public boolean hasProjectNotificationSubscribersForDispatchers(String projectUuid, Collection<String> dispatcherKeys) {
    if (dispatcherKeys.isEmpty()) {
      return false;
    }

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
      "and (" + repeat("pp.prop_key like ?", " or ", dispatcherKeys.size()) + ")";
    PreparedStatement res = connection.prepareStatement(sql);
    res.setString(1, projectUuid);
    int index = 2;
    for (String dispatcherKey : dispatcherKeys) {
      res.setString(index, NOTIFICATION_PREFIX + dispatcherKey + ".%");
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

  @CheckForNull
  public PropertyDto selectProjectProperty(long componentId, String propertyKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectProjectProperty(session, componentId, propertyKey);
    }
  }

  @CheckForNull
  public PropertyDto selectProjectProperty(DbSession dbSession, long componentId, String propertyKey) {
    return getMapper(dbSession).selectByKey(new PropertyDto().setKey(propertyKey).setResourceId(componentId));
  }

  public List<PropertyDto> selectByQuery(PropertyQuery query, DbSession session) {
    return getMapper(session).selectByQuery(query);
  }

  public List<PropertyDto> selectGlobalPropertiesByKeys(DbSession session, Set<String> keys) {
    return executeLargeInputs(keys, partitionKeys -> getMapper(session).selectByKeys(partitionKeys));
  }

  public List<PropertyDto> selectPropertiesByKeysAndComponentIds(DbSession session, Set<String> keys, Set<Long> componentIds) {
    return executeLargeInputs(keys, partitionKeys -> executeLargeInputs(componentIds,
      partitionComponentIds -> getMapper(session).selectByKeysAndComponentIds(partitionKeys, partitionComponentIds)));
  }

  public List<PropertyDto> selectPropertiesByComponentIds(DbSession session, Set<Long> componentIds) {
    return executeLargeInputs(componentIds, getMapper(session)::selectByComponentIds);
  }

  public List<PropertyDto> selectByKeyAndMatchingValue(DbSession session, String key, String value) {
    return getMapper(session).selectByKeyAndMatchingValue(key, value);
  }

  public List<PropertyDto> selectByKeyAndUserIdAndComponentQualifier(DbSession session, String key, int userId, String qualifier) {
    return getMapper(session).selectByKeyAndUserIdAndComponentQualifier(key, userId, qualifier);
  }

  /**
   * Saves the specified property and its value.
   * <p>
   * If {@link PropertyDto#getValue()} is {@code null} or empty, the properties is persisted as empty.
   * </p>
   *
   * @throws IllegalArgumentException if {@link PropertyDto#getKey()} is {@code null} or empty
   */
  public void saveProperty(DbSession session, PropertyDto property) {
    save(getMapper(session), property.getKey(), property.getUserId(), property.getResourceId(), property.getValue());
  }

  private void save(PropertiesMapper mapper,
    String key, @Nullable Integer userId, @Nullable Long componentId,
    @Nullable String value) {
    checkKey(key);

    long now = system2.now();
    mapper.delete(key, userId, componentId);
    if (isEmpty(value)) {
      mapper.insertAsEmpty(key, userId, componentId, now);
    } else if (mustBeStoredInClob(value)) {
      mapper.insertAsClob(key, userId, componentId, value, now);
    } else {
      mapper.insertAsText(key, userId, componentId, value, now);
    }
  }

  private static boolean mustBeStoredInClob(String value) {
    return value.length() > VARCHAR_MAXSIZE;
  }

  private static void checkKey(@Nullable String key) {
    checkArgument(!isEmpty(key), "key can't be null nor empty");
  }

  private static boolean isEmpty(@Nullable String str) {
    return str == null || str.isEmpty();
  }

  public void saveProperty(PropertyDto property) {
    try (DbSession session = mybatis.openSession(false)) {
      saveProperty(session, property);
      session.commit();
    }
  }

  /**
   * Delete either global, user, component or component per user properties.
   * <p>Behaves in exactly the same way as {@link #selectByQuery(PropertyQuery, DbSession)} but deletes rather than
   * selects</p>
   *
   * Used by Governance.
   */
  public int deleteByQuery(DbSession dbSession, PropertyQuery query) {
    return getMapper(dbSession).deleteByQuery(query);
  }

  public int delete(DbSession dbSession, PropertyDto dto) {
    return getMapper(dbSession).delete(dto.getKey(), dto.getUserId(), dto.getResourceId());
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

  public void deleteByOrganizationAndUser(DbSession dbSession, String organizationUuid, int userId) {
    List<Long> ids = getMapper(dbSession).selectIdsByOrganizationAndUser(organizationUuid, userId);
    executeLargeInputsWithoutOutput(ids, subList -> getMapper(dbSession).deleteByIds(subList));
  }

  public void deleteByOrganizationAndMatchingLogin(DbSession dbSession, String organizationUuid, String login, List<String> propertyKeys) {
    List<Long> ids = getMapper(dbSession).selectIdsByOrganizationAndMatchingLogin(organizationUuid, login, propertyKeys);
    executeLargeInputsWithoutOutput(ids, list -> getMapper(dbSession).deleteByIds(list));
  }

  public void deleteByKeyAndValue(DbSession dbSession, String key, String value) {
    getMapper(dbSession).deleteByKeyAndValue(key, value);
  }

  public void saveGlobalProperties(Map<String, String> properties) {
    try (DbSession session = mybatis.openSession(false)) {
      PropertiesMapper mapper = getMapper(session);
      properties.entrySet().forEach(entry -> {
        mapper.deleteGlobalProperty(entry.getKey());
        save(mapper, entry.getKey(), null, null, entry.getValue());
      });
      session.commit();
    }
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
