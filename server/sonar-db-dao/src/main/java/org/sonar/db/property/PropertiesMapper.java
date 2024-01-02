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

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.EmailSubscriberDto;

public interface PropertiesMapper {

  Set<Subscriber> findUsersForNotification(@Param("notifKey") String notificationKey, @Nullable @Param("projectKey") String projectKey);

  Set<EmailSubscriberDto> findEmailRecipientsForNotification(@Param("notifKey") String notificationKey, @Nullable @Param("projectKey") String projectKey,
    @Nullable @Param("logins") List<String> logins);

  List<PropertyDto> selectGlobalProperties();

  PropertyDto selectByKey(PropertyDto key);

  List<PropertyDto> selectByKeys(@Param("keys") List<String> keys);

  List<PropertyDto> selectByKeysAndComponentUuids(@Param("keys") List<String> keys, @Param("componentUuids") List<String> componentUuids);

  List<PropertyDto> selectByKeyAndUserUuidAndComponentQualifier(@Param("key") String key, @Param("userUuid") String userUuid, @Param("qualifier") String qualifier);

  List<PropertyDto> selectByComponentUuids(@Param("componentUuids") List<String> componentUuids);

  List<PropertyDto> selectByQuery(@Param("query") PropertyQuery query);

  List<PropertyDto> selectByKeyAndMatchingValue(@Param("key") String key, @Param("value") String value);

  List<String> selectUuidsByUser(@Param("userUuid") String userUuid);

  List<String> selectIdsByMatchingLogin(@Param("login") String login, @Param("propertyKeys") List<String> propertyKeys);

  void insertAsEmpty(@Param("uuid") String uuid, @Param("key") String key, @Nullable @Param("userUuid") String userUuid, @Nullable @Param("componentUuid") String componentUuid,
    @Param("now") long now);

  void insertAsText(@Param("uuid") String uuid, @Param("key") String key, @Nullable @Param("userUuid") String userUuid, @Nullable @Param("componentUuid") String componentUuid,
    @Param("value") String value, @Param("now") long now);

  void insertAsClob(@Param("uuid") String uuid, @Param("key") String key, @Nullable @Param("userUuid") String userUuid, @Nullable @Param("componentUuid") String componentUuid,
    @Param("value") String value, @Param("now") long now);

  int delete(@Param("key") String key, @Nullable @Param("userUuid") String userUuid, @Nullable @Param("componentUuid") String componentUuid);

  int deleteProjectProperty(@Param("key") String key, @Param("componentUuid") String componentUuid);

  int deleteProjectProperties(@Param("key") String key, @Param("value") String value);

  int deleteGlobalProperty(@Param("key") String key);

  int deleteByQuery(@Param("query") PropertyQuery query);

  void deleteByUuids(@Param("uuids") List<String> uuids);

  int deleteByKeyAndValue(@Param("key") String key, @Param("value") String value);

  List<PropertyDto> selectByUuids(@Param("uuids") List<String> uuids);

  int renamePropertyKey(@Param("oldKey") String oldKey, @Param("newKey") String newKey);

}
