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
package org.sonar.db.component;

import com.google.common.base.Function;
import javax.annotation.Nonnull;

/**
 * Common Functions on ComponentDto.
 */
public final class ComponentDtoFunctions {
  private ComponentDtoFunctions() {
    // prevents instantiation
  }

  public static Function<ComponentDto, String> toKey() {
    return ToKey.INSTANCE;
  }

  private enum ToKey implements Function<ComponentDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDto input) {
      return input.key();
    }
  }

  public static Function<ComponentDto, String> toProjectUuid() {
    return ToProjectUuid.INSTANCE;
  }

  private enum ToProjectUuid implements Function<ComponentDto, String> {
    INSTANCE;

    @Override
    public String apply(ComponentDto input) {
      return input.projectUuid();
    }
  }

  public static Function<ComponentDto, String> toUuid() {
    return ToUuid.INSTANCE;
  }

  private enum ToUuid implements Function<ComponentDto, String> {
    INSTANCE;

    @Override
    public String apply(ComponentDto input) {
      return input.uuid();
    }
  }

  public static Function<ComponentDto, Long> toCopyResourceId() {
    return ToCopyResourceId.INSTANCE;
  }

  private enum ToCopyResourceId implements Function<ComponentDto, Long> {
    INSTANCE;

    @Override
    public Long apply(ComponentDto input) {
      return input.getCopyResourceId();
    }
  }
}
