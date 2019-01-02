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
package org.sonar.ce.task.projectanalysis.duplication;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import org.sonar.ce.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * In-memory implementation of {@link DuplicationRepository}.
 */
public class DuplicationRepositoryImpl implements DuplicationRepository {
  private Multimap<String, Duplication> duplications = HashMultimap.create();

  @Override
  public Iterable<Duplication> getDuplications(Component file) {
    checkFileComponentArgument(file);

    Collection<Duplication> res = this.duplications.asMap().get(file.getDbKey());
    if (res == null) {
      return Collections.emptyList();
    }
    return res;
  }

  @Override
  public void add(Component file, Duplication duplication) {
    checkFileComponentArgument(file);
    checkNotNull(duplication, "duplication can not be null");

    duplications.put(file.getDbKey(), duplication);
  }

  private static void checkFileComponentArgument(Component file) {
    requireNonNull(file, "file can not be null");
    checkArgument(file.getType() == Component.Type.FILE, "type of file must be FILE");
  }

}
