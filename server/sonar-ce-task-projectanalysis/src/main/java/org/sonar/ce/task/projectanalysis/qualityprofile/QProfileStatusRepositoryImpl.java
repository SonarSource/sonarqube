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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class QProfileStatusRepositoryImpl implements MutableQProfileStatusRepository {

  private final Map<String, Status> statuses = new HashMap<>();

  @Override
  public Optional<Status> get(@Nullable String qpKey) {
    return Optional.ofNullable(statuses.get(qpKey));
  }

  @Override
  public void register(String qpKey, Status status) {
    checkNotNull(qpKey, "qpKey can't be null");
    checkNotNull(status, "status can't be null");
    checkState(statuses.put(qpKey, status) == null, "Quality Profile '%s' is already registered", qpKey);
  }
}
