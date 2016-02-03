/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.issue;

import java.util.Collections;
import java.util.List;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.server.computation.component.Component;

/**
 * Factory of {@link Input} of base data for issue tracking. Data are lazy-loaded.
 */
public class TrackerBaseInputFactory {
  private static final LineHashSequence EMPTY_LINE_HASH_SEQUENCE = new LineHashSequence(Collections.<String>emptyList());

  private final BaseIssuesLoader baseIssuesLoader;
  private final DbClient dbClient;

  public TrackerBaseInputFactory(BaseIssuesLoader baseIssuesLoader, DbClient dbClient) {
    this.baseIssuesLoader = baseIssuesLoader;
    this.dbClient = dbClient;
  }

  public Input<DefaultIssue> create(Component component) {
    return new BaseLazyInput(component);
  }

  private class BaseLazyInput extends LazyInput<DefaultIssue> {
    private final Component component;

    private BaseLazyInput(Component component) {
      this.component = component;
    }

    @Override
    protected LineHashSequence loadLineHashSequence() {
      if (component.getType() != Component.Type.FILE) {
        return EMPTY_LINE_HASH_SEQUENCE;
      }
      
      DbSession session = dbClient.openSession(false);
      try {
        List<String> hashes = dbClient.fileSourceDao().selectLineHashes(session, component.getUuid());
        if (hashes == null || hashes.isEmpty()) {
          return EMPTY_LINE_HASH_SEQUENCE;
        }
        return new LineHashSequence(hashes);
      } finally {
        MyBatis.closeQuietly(session);
      }
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      return baseIssuesLoader.loadForComponentUuid(component.getUuid());
    }
  }
}
