/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.notifications;

import com.google.common.collect.Maps;
import org.sonar.api.ServerExtension;

import java.util.Map;

/**
 * <p>
 * Notification dispatchers (see {@link NotificationDispatcher}) can define their own metadata class in order
 * to tell more about them. 
 * <br/>
 * Instances of those classes must be passed to Pico container (generally in the 
 * {@link SonarPlugin#getExtensions()} method implementation).
 * </p> 
 * 
 * @since 3.5
 */
public final class NotificationDispatcherMetadata implements ServerExtension {

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

}
