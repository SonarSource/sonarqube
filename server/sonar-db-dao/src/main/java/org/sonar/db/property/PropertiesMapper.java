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

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface PropertiesMapper {

  Set<Subscriber> findUsersForNotification(@Param("notifKey") String notificationKey, @Nullable @Param("projectKey") String projectKey);

  List<PropertyDto> selectGlobalProperties();

  List<PropertyDto> selectProjectProperties(String resourceKey);

  PropertyDto selectByKey(PropertyDto key);

  List<PropertyDto> selectByKeys(@Param("keys") List<String> keys);

  List<PropertyDto> selectByKeysAndComponentIds(@Param("keys") List<String> keys, @Param("componentIds") List<Long> componentIds);

  List<PropertyDto> selectByKeyAndUserIdAndComponentQualifier(@Param("key") String key, @Param("userId") int userId, @Param("qualifier") String qualifier);

  List<PropertyDto> selectByComponentIds(@Param("componentIds") List<Long> componentIds);

  List<PropertyDto> selectByQuery(@Param("query") PropertyQuery query);

  List<PropertyDto> selectByKeyAndMatchingValue(@Param("key") String key, @Param("value") String value);

  List<Long> selectIdsByOrganizationAndUser(@Param("organizationUuid") String organizationUuid, @Param("userId") int userId);

  List<Long> selectIdsByOrganizationAndMatchingLogin(@Param("organizationUuid") String organizationUuid, @Param("login") String login,
                                                     @Param("propertyKeys") List<String> propertyKeys);

  List<PropertyDto> selectGlobalPropertiesByKeyQuery(@Param("textQuery") String textQuery);

  void insertAsEmpty(@Param("key") String key, @Nullable @Param("userId") Integer userId, @Nullable @Param("componentId") Long componentId,
    @Param("now") long now);

  void insertAsText(@Param("key") String key, @Nullable @Param("userId") Integer userId, @Nullable @Param("componentId") Long componentId,
    @Param("value") String value, @Param("now") long now);

  void insertAsClob(@Param("key") String key, @Nullable @Param("userId") Integer userId, @Nullable @Param("componentId") Long componentId,
    @Param("value") String value, @Param("now") long now);

  int delete(@Param("key") String key, @Nullable @Param("userId") Integer userId, @Nullable @Param("componentId") Long componentId);

  int deleteById(long id);

  int deleteProjectProperty(@Param("key") String key, @Param("rId") Long componentId);

  int deleteProjectProperties(@Param("key") String key, @Param("value") String value);

  int deleteGlobalProperty(@Param("key") String key);

  int deleteByQuery(@Param("query") PropertyQuery query);

  void deleteByIds(@Param("ids") List<Long> ids);

  void deleteByKeyAndValue(@Param("key") String key, @Param("value") String value);

  int renamePropertyKey(@Param("oldKey") String oldKey, @Param("newKey") String newKey);

}
