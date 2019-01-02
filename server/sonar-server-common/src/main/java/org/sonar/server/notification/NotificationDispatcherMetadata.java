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
package org.sonar.server.notification;

import com.google.common.collect.Maps;
import java.util.Map;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * Notification dispatchers (see {@link NotificationDispatcher}) can define their own metadata class in order
 * to tell more about them.
 * <p>
 * Instances of these classes must be declared by {@link org.sonar.api.Plugin}.
 * </p>
 */
@ServerSide
@ComputeEngineSide
public final class NotificationDispatcherMetadata {

  public static final String GLOBAL_NOTIFICATION = "globalNotification";
  public static final String PER_PROJECT_NOTIFICATION = "perProjectNotification";

  private String dispatcherKey;
  private Map<String, String> properties;

  private NotificationDispatcherMetadata(String dispatcherKey) {
    this.dispatcherKey = dispatcherKey;
    this.properties = Maps.newHashMap();
  }

  /**
   * Creates a new metadata instance for the given dispatcher.
   * <p/>
   * By default the key is the class name without package. It can be changed by overriding
   * {@link NotificationDispatcher#getKey()}.
   */
  public static NotificationDispatcherMetadata create(String dispatcherKey) {
    return new NotificationDispatcherMetadata(dispatcherKey);
  }

  /**
   * Sets a property on this metadata object.
   */
  public NotificationDispatcherMetadata setProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  /**
   * Gives the property for the given key.
   */
  public String getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Returns the unique key of the dispatcher.
   */
  public String getDispatcherKey() {
    return dispatcherKey;
  }

  @Override
  public String toString() {
    return dispatcherKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationDispatcherMetadata that = (NotificationDispatcherMetadata) o;
    return dispatcherKey.equals(that.dispatcherKey);
  }

  @Override
  public int hashCode() {
    return dispatcherKey.hashCode();
  }
}
