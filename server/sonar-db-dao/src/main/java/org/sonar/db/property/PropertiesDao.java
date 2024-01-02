/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.MyBatis;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsIntoSet;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class PropertiesDao implements Dao {

  private static final String NOTIFICATION_PREFIX = "notification.";
  private static final int VARCHAR_MAXSIZE = 4000;

  private final MyBatis mybatis;
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public PropertiesDao(MyBatis mybatis, System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.mybatis = mybatis;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  /**
   * Returns the logins of users who have subscribed to the given notification dispatcher with the given notification channel.
   * If a resource ID is passed, the search is made on users who have specifically subscribed for the given resource.
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
      "left outer join components pj on pp.component_uuid = pj.uuid " +
      "where pp.user_uuid is not null and (pp.component_uuid is null or pj.uuid=?) " +
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

  public List<PropertyDto> selectComponentProperties(DbSession session, String componentUuid) {
    return getMapper(session).selectByComponentUuids(singletonList(componentUuid));
  }

  public List<PropertyDto> selectComponentProperties(String componentUuid) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectComponentProperties(session, componentUuid);
    }
  }

  @CheckForNull
  public PropertyDto selectProjectProperty(String projectUuid, String propertyKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectProjectProperty(session, projectUuid, propertyKey);
    }
  }

  @CheckForNull
  public PropertyDto selectProjectProperty(DbSession dbSession, String projectUuid, String propertyKey) {
    return getMapper(dbSession).selectByKey(new PropertyDto().setKey(propertyKey).setComponentUuid(projectUuid));
  }

  public List<PropertyDto> selectByQuery(PropertyQuery query, DbSession session) {
    return getMapper(session).selectByQuery(query);
  }

  public List<PropertyDto> selectGlobalPropertiesByKeys(DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, partitionKeys -> getMapper(session).selectByKeys(partitionKeys));
  }

  public List<PropertyDto> selectPropertiesByKeysAndComponentUuids(DbSession session, Collection<String> keys, Collection<String> componentUuids) {
    return executeLargeInputs(keys, partitionKeys -> executeLargeInputs(componentUuids,
      partitionComponentUuids -> getMapper(session).selectByKeysAndComponentUuids(partitionKeys, partitionComponentUuids)));
  }

  public List<PropertyDto> selectPropertiesByComponentUuids(DbSession session, Collection<String> componentUuids) {
    return executeLargeInputs(componentUuids, getMapper(session)::selectByComponentUuids);
  }

  public List<PropertyDto> selectByKeyAndMatchingValue(DbSession session, String key, String value) {
    return getMapper(session).selectByKeyAndMatchingValue(key, value);
  }

  public List<PropertyDto> selectByKeyAndUserUuidAndComponentQualifier(DbSession session, String key, String userUuid, String qualifier) {
    return getMapper(session).selectByKeyAndUserUuidAndComponentQualifier(key, userUuid, qualifier);
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
    saveProperty(session, property, null, null, null, null);
  }

  public void saveProperty(DbSession session, PropertyDto property, @Nullable String userLogin, @Nullable String projectKey, @Nullable String projectName,
    @Nullable String qualifier) {
    int affectedRows = save(getMapper(session), property.getKey(), property.getUserUuid(), property.getComponentUuid(), property.getValue());

    if (affectedRows > 0) {
      auditPersister.updateProperty(session, new PropertyNewValue(property, userLogin, projectKey, projectName, qualifier), false);
    } else {
      auditPersister.addProperty(session, new PropertyNewValue(property, userLogin, projectKey, projectName, qualifier), false);
    }
  }

  private int save(PropertiesMapper mapper, String key, @Nullable String userUuid, @Nullable String componentUuid, @Nullable String value) {
    checkKey(key);

    long now = system2.now();
    int affectedRows = mapper.delete(key, userUuid, componentUuid);
    String uuid = uuidFactory.create();
    if (isEmpty(value)) {
      mapper.insertAsEmpty(uuid, key, userUuid, componentUuid, now);
    } else if (mustBeStoredInClob(value)) {
      mapper.insertAsClob(uuid, key, userUuid, componentUuid, value, now);
    } else {
      mapper.insertAsText(uuid, key, userUuid, componentUuid, value, now);
    }
    return affectedRows;
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
   * Used by Governance.
   */
  public int deleteByQuery(DbSession dbSession, PropertyQuery query) {
    int deletedRows = getMapper(dbSession).deleteByQuery(query);

    if (deletedRows > 0 && query.key() != null) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(query.key(), query.componentUuid(),
        null, null, null, query.userUuid()), false);
    }

    return deletedRows;
  }

  public int delete(DbSession dbSession, PropertyDto dto, @Nullable String userLogin, @Nullable String projectKey,
    @Nullable String projectName, @Nullable String qualifier) {
    int deletedRows = getMapper(dbSession).delete(dto.getKey(), dto.getUserUuid(), dto.getComponentUuid());

    if (deletedRows > 0) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(dto, userLogin, projectKey, projectName, qualifier),
        false);
    }
    return deletedRows;
  }

  public void deleteProjectProperty(String key, String projectUuid, String projectKey, String projectName, String qualifier) {
    try (DbSession session = mybatis.openSession(false)) {
      deleteProjectProperty(session, key, projectUuid, projectKey, projectName, qualifier);
      session.commit();
    }
  }

  public void deleteProjectProperty(DbSession session, String key, String projectUuid, String projectKey,
    String projectName, String qualifier) {
    int deletedRows = getMapper(session).deleteProjectProperty(key, projectUuid);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(session, new PropertyNewValue(key, projectUuid, projectKey, projectName, qualifier,
        null), false);
    }
  }

  public void deleteProjectProperties(String key, String value, DbSession session) {
    int deletedRows = getMapper(session).deleteProjectProperties(key, value);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(session, new PropertyNewValue(key, value), false);
    }
  }

  public void deleteProjectProperties(String key, String value) {
    try (DbSession session = mybatis.openSession(false)) {
      deleteProjectProperties(key, value, session);
      session.commit();
    }
  }

  public void deleteGlobalProperty(String key, DbSession session) {
    int deletedRows = getMapper(session).deleteGlobalProperty(key);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(session, new PropertyNewValue(key), false);
    }
  }

  public void deleteGlobalProperty(String key) {
    try (DbSession session = mybatis.openSession(false)) {
      deleteGlobalProperty(key, session);
      session.commit();
    }
  }

  public void deleteByUser(DbSession dbSession, String userUuid, String userLogin) {
    List<String> uuids = getMapper(dbSession).selectUuidsByUser(userUuid);

    persistDeletedProperties(dbSession, userUuid, userLogin, uuids);

    executeLargeInputsWithoutOutput(uuids, subList -> getMapper(dbSession).deleteByUuids(subList));
  }

  public void deleteByMatchingLogin(DbSession dbSession, String login, List<String> propertyKeys) {
    List<String> uuids = getMapper(dbSession).selectIdsByMatchingLogin(login, propertyKeys);

    persistDeletedProperties(dbSession, null, login, uuids);

    executeLargeInputsWithoutOutput(uuids, list -> getMapper(dbSession).deleteByUuids(list));
  }

  public void deleteByKeyAndValue(DbSession dbSession, String key, String value) {
    int deletedRows = getMapper(dbSession).deleteByKeyAndValue(key, value);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(key, value), false);
    }
  }

  public void saveGlobalProperties(Map<String, String> properties) {
    try (DbSession session = mybatis.openSession(false)) {
      PropertiesMapper mapper = getMapper(session);
      properties.forEach((key, value) -> {
        int affectedRows = save(mapper, key, null, null, value);

        if (auditPersister.isTrackedProperty(key)) {
          if (affectedRows > 0) {
            auditPersister.updateProperty(session, new PropertyNewValue(key, value), false);
          } else {
            auditPersister.addProperty(session, new PropertyNewValue(key, value), false);
          }
        }
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

  private void persistDeletedProperties(DbSession dbSession, @Nullable String userUuid, String userLogin, List<String> uuids) {
    if (!uuids.isEmpty()) {
      List<PropertyDto> properties = executeLargeInputs(uuids, subList -> getMapper(dbSession).selectByUuids(subList));

      properties
        .stream()
        .filter(p -> auditPersister.isTrackedProperty(p.getKey()))
        .forEach(p -> auditPersister.deleteProperty(dbSession,
          new PropertyNewValue(p.getKey(), userUuid, userLogin), false));
    }
  }
}
