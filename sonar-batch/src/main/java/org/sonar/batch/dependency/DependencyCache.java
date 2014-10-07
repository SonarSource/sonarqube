/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.dependency;

import com.google.common.base.Preconditions;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;

import javax.annotation.CheckForNull;

/**
 * Cache of all dependencies. This cache is shared amongst all project modules.
 * module key -> from key -> to key -> OutgoingDependency
 */
public class DependencyCache implements BatchComponent {

  private final Cache<OutgoingDependency> cache;

  public DependencyCache(Caches caches) {
    cache = caches.createCache("dependencies");
  }

  public Iterable<Entry<OutgoingDependency>> entries() {
    return cache.entries();
  }

  @CheckForNull
  public OutgoingDependency get(String moduleKey, InputFile from, InputFile to) {
    Preconditions.checkNotNull(moduleKey);
    Preconditions.checkNotNull(from);
    Preconditions.checkNotNull(to);
    return cache.get(moduleKey, ((DefaultInputFile) from).key(), ((DefaultInputFile) to).key());
  }

  public DependencyCache put(String moduleKey, InputFile from, OutgoingDependency dependency) {
    Preconditions.checkNotNull(moduleKey);
    Preconditions.checkNotNull(from);
    Preconditions.checkNotNull(dependency);
    cache.put(moduleKey, ((DefaultInputFile) from).key(), ((DefaultInputFile) dependency.to()).key(), dependency);
    return this;
  }

}
