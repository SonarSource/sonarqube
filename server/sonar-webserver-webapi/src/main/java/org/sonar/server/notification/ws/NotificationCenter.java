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
package org.sonar.server.notification.ws;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.notification.NotificationDispatcherMetadata;

public class NotificationCenter {

  private static final Logger LOG = Loggers.get(NotificationCenter.class);

  private final NotificationDispatcherMetadata[] dispatchersMetadata;
  private final NotificationChannel[] channels;

  public NotificationCenter(NotificationDispatcherMetadata[] metadata, NotificationChannel[] channels) {
    this.dispatchersMetadata = metadata;
    this.channels = channels;
  }

  /**
   * Default constructor when no channels.
   */
  public NotificationCenter(NotificationDispatcherMetadata[] metadata) {
    this(metadata, new NotificationChannel[0]);
    LOG.warn("There is no notification channel - no notification will be delivered!");
  }

  /**
   * Default constructor when no dispatcher metadata.
   */
  public NotificationCenter(NotificationChannel[] channels) {
    this(new NotificationDispatcherMetadata[0], channels);
  }

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

}
