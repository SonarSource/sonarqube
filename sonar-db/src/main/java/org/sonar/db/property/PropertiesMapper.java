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

package org.sonar.db.property;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface PropertiesMapper {

  List<String> findUsersForNotification(@Param("notifKey") String notificationKey, @Nullable @Param("projectUuid") String projectUuid);

  List<String> findNotificationSubscribers(@Param("propKey") String propertyKey, @Nullable @Param("componentKey") String componentKey);

  List<PropertyDto> selectGlobalProperties();

  List<PropertyDto> selectProjectProperties(String resourceKey);

  List<PropertyDto> selectProjectPropertiesByResourceId(Long resourceId);

  List<PropertyDto> selectSetOfResourceProperties(@Param("rId") Long projectId, @Param("propKeys") List<String> propertyKeys);

  PropertyDto selectByKey(PropertyDto key);

  List<PropertyDto> selectByQuery(@Param("query") PropertyQuery query);

  List<PropertyDto> selectDescendantModuleProperties(@Param("moduleUuid") String moduleUuid, @Param(value = "scope") String scope,
    @Param(value = "excludeDisabled") boolean excludeDisabled);

  void update(PropertyDto property);

  void insert(PropertyDto property);

  void deleteProjectProperty(@Param("key") String key, @Param("rId") Long resourceId);

  void deleteProjectProperties(@Param("key") String key, @Param("value") String value);

  void deleteGlobalProperty(String key);

  void deleteAllProperties(String key);

  void deleteGlobalProperties();

  void renamePropertyKey(@Param("oldKey") String oldKey, @Param("newKey") String newKey);

  void updateProperties(@Param("key") String key, @Param("oldValue") String oldValue, @Param("newValue") String newValue);
}
