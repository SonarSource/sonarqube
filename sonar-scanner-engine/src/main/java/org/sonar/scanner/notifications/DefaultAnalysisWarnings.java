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
package org.sonar.scanner.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;

import static com.google.common.base.Preconditions.checkArgument;

public class DefaultAnalysisWarnings implements AnalysisWarnings {
  private final System2 system2;

  private final List<Message> messages = new ArrayList<>();
  private final Set<String> seen = new HashSet<>();

  public DefaultAnalysisWarnings(System2 system2) {
    this.system2 = system2;
  }

  @Override
  public void addUnique(String text) {
    if (this.seen.add(text)) {
      this.messages.add(new Message(text, system2.now()));
    }
  }

  public List<Message> warnings() {
    return Collections.unmodifiableList(messages);
  }

  @Immutable
  public static class Message {
    private final String text;
    private final long timestamp;

    Message(String text, long timestamp) {
      checkArgument(!text.isEmpty(), "Text can't be empty");
      this.text = text;
      this.timestamp = timestamp;
    }

    public String getText() {
      return text;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
