/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.server.notifications;

import org.sonar.api.ServerComponent;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @since 2.10
 */
public class NotificationQueue implements ServerComponent {

  private ConcurrentLinkedQueue<Element> queue = new ConcurrentLinkedQueue<Element>();

  public static class Element {
    String channelKey;
    Serializable notificationData;

    public Element(String channelKey, Serializable notificationData) {
      this.channelKey = channelKey;
      this.notificationData = notificationData;
    }
  }

  public Element get() {
    return queue.poll();
  }

  public void add(Serializable notificationData, NotificationChannel channel) {
    queue.add(new Element(channel.getKey(), notificationData));
  }

}
