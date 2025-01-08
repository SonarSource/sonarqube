/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.MyBatis;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsIntoSet;

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

  public Set<EmailSubscriberDto> findEnabledEmailSubscribersForNotification(DbSession dbSession, String notificationDispatcherKey, String notificationChannelKey,
    @Nullable String projectKey) {
    return getMapper(dbSession).findEmailRecipientsForNotification(NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey, projectKey, null,
      Boolean.toString(true));
  }

  public Set<EmailSubscriberDto> findEnabledEmailSubscribersForNotification(DbSession dbSession, String notificationDispatcherKey, String notificationChannelKey,
    @Nullable String projectKey, Set<String> logins) {
    return findEmailSubscribersForNotification(dbSession, notificationDispatcherKey, notificationChannelKey, projectKey, logins, true);
  }

  public Set<EmailSubscriberDto> findDisabledEmailSubscribersForNotification(DbSession dbSession, String notificationDispatcherKey, String notificationChannelKey,
    @Nullable String projectKey, Set<String> logins) {
    return findEmailSubscribersForNotification(dbSession, notificationDispatcherKey, notificationChannelKey, projectKey, logins, false);
  }

  public Set<EmailSubscriberDto> findEmailSubscribersForNotification(DbSession dbSession, String notificationDispatcherKey, String notificationChannelKey,
    @Nullable String projectKey, Set<String> logins, boolean enabled) {
    if (logins.isEmpty()) {
      return Collections.emptySet();
    }

    return executeLargeInputsIntoSet(
      logins,
      loginsPartition -> {
        String notificationKey = NOTIFICATION_PREFIX + notificationDispatcherKey + "." + notificationChannelKey;
        return getMapper(dbSession).findEmailRecipientsForNotification(notificationKey, projectKey, loginsPartition, Boolean.toString(enabled));
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
      "where pp.user_uuid is not null and (pp.entity_uuid is null or pp.entity_uuid=?) " +
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

  public List<PropertyDto> selectEntityProperties(DbSession session, String entityUuid) {
    return getMapper(session).selectByEntityUuids(singletonList(entityUuid));
  }

  @CheckForNull
  public PropertyDto selectProjectProperty(DbSession dbSession, String projectUuid, String propertyKey) {
    return getMapper(dbSession).selectByKey(new PropertyDto().setKey(propertyKey).setEntityUuid(projectUuid));
  }

  public Optional<PropertyDto> selectProjectProperty(String projectUuid, String propertyKey) {
    try (DbSession session = mybatis.openSession(false)) {
      return Optional.ofNullable(selectProjectProperty(session, projectUuid, propertyKey));
    }
  }

  public List<PropertyDto> selectByQuery(PropertyQuery query, DbSession session) {
    return getMapper(session).selectByQuery(query);
  }

  public List<PropertyDto> selectGlobalPropertiesByKeys(DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, partitionKeys -> getMapper(session).selectByKeys(partitionKeys));
  }

  public List<PropertyDto> selectPropertiesByKeysAndEntityUuids(DbSession session, Collection<String> keys, Collection<String> entityUuids) {
    return executeLargeInputs(keys, partitionKeys -> executeLargeInputs(entityUuids,
      partitionEntityUuids -> getMapper(session).selectByKeysAndEntityUuids(partitionKeys, partitionEntityUuids)));
  }

  public List<PropertyDto> selectByKeyAndMatchingValue(DbSession session, String key, String value) {
    return getMapper(session).selectByKeyAndMatchingValue(key, value);
  }

  public List<PropertyDto> selectEntityPropertyByKeyAndUserUuid(DbSession session, String key, String userUuid) {
    return getMapper(session).selectEntityPropertyByKeyAndUserUuid(key, userUuid);
  }

  public List<PropertyDto> selectProjectPropertyByKey(DbSession session, String key) {
    return getMapper(session).selectProjectPropertyByKey(key);
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
    int affectedRows = save(getMapper(session), property.getKey(), property.getUserUuid(), property.getEntityUuid(), property.getValue());

    if (affectedRows > 0) {
      auditPersister.updateProperty(session, new PropertyNewValue(property, userLogin, projectKey, projectName, qualifier), false);
    } else {
      auditPersister.addProperty(session, new PropertyNewValue(property, userLogin, projectKey, projectName, qualifier), false);
    }
  }

  private int save(PropertiesMapper mapper, String key, @Nullable String userUuid, @Nullable String entityUuids, @Nullable String value) {
    checkKey(key);

    long now = system2.now();
    int affectedRows = mapper.delete(key, userUuid, entityUuids);
    String uuid = uuidFactory.create();
    if (isEmpty(value)) {
      mapper.insertAsEmpty(uuid, key, userUuid, entityUuids, now);
    } else if (mustBeStoredInClob(value)) {
      mapper.insertAsClob(uuid, key, userUuid, entityUuids, value, now);
    } else {
      mapper.insertAsText(uuid, key, userUuid, entityUuids, value, now);
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
   * Delete either global, user, entity or entity per user properties.
   * <p>Behaves in exactly the same way as {@link #selectByQuery(PropertyQuery, DbSession)} but deletes rather than
   * selects</p>
   * Used by Governance.
   */
  public int deleteByQuery(DbSession dbSession, PropertyQuery query) {
    int deletedRows = getMapper(dbSession).deleteByQuery(query);

    if (deletedRows > 0 && query.key() != null) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(query.key(), query.entityUuid(),
        null, null, null, query.userUuid()), false);
    }

    return deletedRows;
  }

  public int delete(DbSession dbSession, PropertyDto dto, @Nullable String userLogin, @Nullable String projectKey,
    @Nullable String projectName, @Nullable String qualifier) {
    int deletedRows = getMapper(dbSession).delete(dto.getKey(), dto.getUserUuid(), dto.getEntityUuid());

    if (deletedRows > 0) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(dto, userLogin, projectKey, projectName, qualifier), false);
    }
    return deletedRows;
  }

  public void deleteProjectProperty(DbSession session, String key, String projectUuid, String projectKey, String projectName, String qualifier) {
    int deletedRows = getMapper(session).deleteProjectProperty(key, projectUuid);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(session, new PropertyNewValue(key, projectUuid, projectKey, projectName, qualifier, null), false);
    }
  }

  public void deleteGlobalProperty(String key, DbSession session) {
    int deletedRows = getMapper(session).deleteGlobalProperty(key);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(session, new PropertyNewValue(key), false);
    }
  }

  public void deleteByKeyAndValue(DbSession dbSession, String key, String value) {
    int deletedRows = getMapper(dbSession).deleteByKeyAndValue(key, value);

    if (deletedRows > 0) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(key, value), false);
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
