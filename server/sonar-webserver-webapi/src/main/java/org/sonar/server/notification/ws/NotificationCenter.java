/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.notification.ws;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.notification.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationCenter {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationCenter.class);

  private final NotificationDispatcherMetadata[] dispatchersMetadata;
  private final NotificationChannel[] channels;

  @Autowired(required = false)
  public NotificationCenter(NotificationDispatcherMetadata[] metadata, NotificationChannel[] channels) {
    this.dispatchersMetadata = metadata;
    this.channels = channels;
  }

  /**
   * Default constructor when no channels.
   */
  @Autowired(required = false)
  public NotificationCenter(NotificationDispatcherMetadata[] metadata) {
    this(metadata, new NotificationChannel[0]);
    LOG.warn("There is no notification channel - no notification will be delivered!");
  }

  /**
   * Default constructor when no dispatcher metadata.
   */
  @Autowired(required = false)
  public NotificationCenter(NotificationChannel[] channels) {
    this(new NotificationDispatcherMetadata[0], channels);
  }

  @Autowired(required = false)
  public NotificationCenter() {
    this(new NotificationDispatcherMetadata[0], new NotificationChannel[0]);
    LOG.warn("There is no notification channel - no notification will be delivered!");
  }

  public List<NotificationChannel> getChannels() {
    return Arrays.asList(channels);
  }

  /**
   * Returns all the available dispatchers which metadata matches the given property and its value.
   * <br/>
   * If "propertyValue" is null, the verification is done on the existence of such a property (whatever the value).
   */
  public List<String> getDispatcherKeysForProperty(String propertyKey, @Nullable String propertyValue) {
    ImmutableList.Builder<String> keys = ImmutableList.builder();
    for (NotificationDispatcherMetadata metadata : dispatchersMetadata) {
      String dispatcherKey = metadata.getDispatcherKey();
      String value = metadata.getProperty(propertyKey);
      if (value != null && (propertyValue == null || value.equals(propertyValue))) {
        keys.add(dispatcherKey);
      }
    }
    return keys.build();
  }

  public Map<String, String> getValueByDispatchers(String propertyKey) {
    Map<String, String> valueByDispatchers = new java.util.HashMap<>();
    for (NotificationDispatcherMetadata metadata : dispatchersMetadata) {
      String dispatcherKey = metadata.getDispatcherKey();
      String value = metadata.getProperty(propertyKey);
      if (value != null) {
        valueByDispatchers.put(dispatcherKey, value);
      }
    }
    return valueByDispatchers;
  }

}
