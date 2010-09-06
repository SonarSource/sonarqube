/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch.indexer;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.HashMap;
import java.util.Map;

public final class ResourcePersisters {

  private Map<Class<? extends Resource>, ResourcePersister> persistersByClass;
  private ResourcePersister defaultPersister;

  public ResourcePersisters(DatabaseSession session) {
    defaultPersister = new DefaultPersister(session);
    persistersByClass = new HashMap<Class<? extends Resource>, ResourcePersister>();
    persistersByClass.put(Project.class, new ProjectPersister(session));
    persistersByClass.put(Library.class, new LibraryPersister(session));
  }

  public ResourcePersister get(Bucket bucket) {
    return get(bucket.getResource());
  }

  public ResourcePersister get(Resource resource) {
    ResourcePersister persister = persistersByClass.get(resource.getClass());
    return persister != null ? persister : defaultPersister;
  }
}
