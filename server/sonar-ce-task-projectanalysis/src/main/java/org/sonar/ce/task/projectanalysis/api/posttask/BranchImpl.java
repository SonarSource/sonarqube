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
package org.sonar.ce.task.projectanalysis.api.posttask;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.posttask.Branch;

@Immutable
public class BranchImpl implements Branch {
  private final boolean isMain;
  @Nullable
  private final String name;
  private final Type type;

  public BranchImpl(boolean isMain, @Nullable String name, Type type) {
    this.isMain = isMain;
    this.name = name;
    this.type = type;
  }

  @Override
  public boolean isMain() {
    return isMain;
  }

  @Override
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Branch{");
    sb.append("isMain=").append(isMain);
    sb.append(", name='").append(name).append('\'');
    sb.append(", type=").append(type);
    sb.append('}');
    return sb.toString();
  }
}
