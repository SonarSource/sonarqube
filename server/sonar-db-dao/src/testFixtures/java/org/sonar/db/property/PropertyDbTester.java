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

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.entity.EntityDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;

public class PropertyDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public PropertyDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public PropertyDto insertProperty(PropertyDto property, @Nullable String componentKey,
    @Nullable String componentName, @Nullable String qualifier, @Nullable String userLogin) {
    dbClient.propertiesDao().saveProperty(db.getSession(), property, userLogin, componentKey, componentName, qualifier);
    db.commit();

    return property;
  }

  public void insertProperties(@Nullable String userLogin, @Nullable String projectKey,
    @Nullable String projectName, @Nullable String qualifier, PropertyDto... properties) {
    insertProperties(asList(properties), userLogin, projectKey, projectName, qualifier);
  }

  public void insertProperties(List<PropertyDto> properties, @Nullable String userLogin, @Nullable String projectKey,
    @Nullable String projectName, @Nullable String qualifier) {
    for (PropertyDto propertyDto : properties) {
      dbClient.propertiesDao().saveProperty(db.getSession(), propertyDto, userLogin, projectKey, projectName, qualifier);
    }
    db.commit();
  }

  public void insertProperty(String propKey, String propValue, @Nullable String componentUuid) {
    insertProperties(singletonList(new PropertyDto()
        .setKey(propKey)
        .setValue(propValue)
        .setEntityUuid(componentUuid)),
      null, null, null, null);
  }

  public void insertPropertySet(String settingBaseKey, @Nullable EntityDto entity, Map<String, String>... fieldValues) {
    int index = 1;
    List<PropertyDto> propertyDtos = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    for (Map<String, String> fieldValue : fieldValues) {
      for (Map.Entry<String, String> entry : fieldValue.entrySet()) {
        String key = settingBaseKey + "." + index + "." + entry.getKey();
        if (entity != null) {
          propertyDtos.add(newComponentPropertyDto(entity).setKey(key).setValue(entry.getValue()));
        } else {
          propertyDtos.add(newGlobalPropertyDto().setKey(key).setValue(entry.getValue()));
        }
      }
      ids.add(Integer.toString(index));
      index++;
    }
    String idsValue = Joiner.on(",").join(ids);
    if (entity != null) {
      propertyDtos.add(newComponentPropertyDto(entity).setKey(settingBaseKey).setValue(idsValue));
    } else {
      propertyDtos.add(newGlobalPropertyDto().setKey(settingBaseKey).setValue(idsValue));
    }
    String componentKey = entity == null ? null : entity.getKey();
    String componentName = entity == null ? null : entity.getName();
    String qualififer = entity == null ? null : entity.getQualifier();
    insertProperties(propertyDtos, null, componentKey, componentName, qualififer);
  }

  public Optional<PropertyDto> findFirstUserProperty(String userUuid, String key) {
    PropertyQuery query = new PropertyQuery.Builder()
      .setUserUuid(userUuid)
      .setKey(key)
      .build();

    return dbClient.propertiesDao().selectByQuery(query, db.getSession()).stream().findFirst();
  }
}
