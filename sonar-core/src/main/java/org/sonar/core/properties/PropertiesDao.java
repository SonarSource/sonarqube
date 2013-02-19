/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class PropertiesDao implements BatchComponent, ServerComponent {

  private MyBatis mybatis;

  public PropertiesDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * Returns the logins of users who have subscribed to the given notification dispatcher with the given notification channel.
   * If a resource ID is passed, the search is made on users who have specifically subscribed for the given resource.
   *
   * @param notificationDispatcherKey the key of the notification dispatcher
   * @param notificationChannelKey the key of the notification channel
   * @param resourceId the resource id
   * @return the list of logins (maybe be empty - obviously)
   */
  public List<String> findUsersForNotification(String notificationDispatcherKey, String notificationChannelKey, @Nullable Long resourceId) {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.findUsersForNotification("notification." + notificationDispatcherKey + "." + notificationChannelKey, resourceId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectGlobalProperties() {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.selectGlobalProperties();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PropertyDto selectGlobalProperty(String propertyKey) {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.selectByKey(new PropertyDto().setKey(propertyKey));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PropertyDto> selectProjectProperties(String resourceKey) {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      return mapper.selectProjectProperties(resourceKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void setProperty(PropertyDto property) {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      PropertyDto persistedProperty = mapper.selectByKey(property);
      if (persistedProperty != null && !StringUtils.equals(persistedProperty.getValue(), property.getValue())) {
        persistedProperty.setValue(property.getValue());
        mapper.update(persistedProperty);
        session.commit();

      } else if (persistedProperty == null) {
        mapper.insert(property);
        session.commit();
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteGlobalProperties() {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      mapper.deleteGlobalProperties();
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteGlobalProperty(String key) {
    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      mapper.deleteGlobalProperty(key);
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void saveGlobalProperties(Map<String, String> properties) {
    SqlSession session = mybatis.openBatchSession();
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
      SqlSession session = mybatis.openSession();
      PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
      try {
        mapper.renamePropertyKey(oldKey, newKey);
        session.commit();

      } finally {
        MyBatis.closeQuietly(session);
      }
    }
  }
}
