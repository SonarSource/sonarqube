/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.component;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class ComponentRepositoryImpl implements MutableComponentRepository {
  private final Map<String, Long> refsByUuid = new HashMap<>();
  private final Set<String> fileUuids = new HashSet<>();

  @Override
  public void register(long ref, String uuid, boolean file) {
    requireNonNull(uuid, "uuid can not be null");
    Long existingRef = refsByUuid.get(uuid);
    if (existingRef != null) {
      checkArgument(ref == existingRef, "Uuid '%s' already registered under ref '%s' in repository", uuid, existingRef);
      boolean existingIsFile = fileUuids.contains(uuid);
      checkArgument(file == existingIsFile, "Uuid '%s' already registered but %sas a File", uuid, existingIsFile ? "" : "not ");
    } else {
      refsByUuid.put(uuid, ref);
      if (file) {
        fileUuids.add(uuid);
      }
    }
  }

  @Override
  public long getRef(String uuid) {
    Long ref = refsByUuid.get(requireNonNull(uuid, "uuid can not be null"));
    checkState(ref != null, "No reference registered in the repository for uuid '%s'", uuid);

    return ref;
  }

  @Override
  public Set<String> getFileUuids() {
    return ImmutableSet.copyOf(fileUuids);
  }
}
