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
package org.sonar.server.duplication.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;

public class Duplication {
  private final ComponentDto componentDto;
  private final String componentDbKey;
  private final Integer from;
  private final Integer size;
  private final boolean removed;

  private Duplication(@Nullable ComponentDto componentDto, String componentDbKey, Integer from, Integer size, boolean removed) {
    this.componentDto = componentDto;
    this.componentDbKey = componentDbKey;
    this.from = from;
    this.size = size;
    this.removed = removed;
  }

  static Duplication newRemovedComponent(String componentDbKey, Integer from, Integer size) {
    return new Duplication(null, componentDbKey, from, size, true);
  }

  static Duplication newTextComponent(String componentDbKey, Integer from, Integer size) {
    return new Duplication(null, componentDbKey, from, size, false);
  }

  static Duplication newComponent(ComponentDto componentDto, Integer from, Integer size) {
    return new Duplication(componentDto, componentDto.getDbKey(), from, size, false);
  }

  String componentDbKey() {
    return componentDbKey;
  }

  Integer from() {
    return from;
  }

  Integer size() {
    return size;
  }

  public boolean removed() {
    return removed;
  }

  /**
   * can be null if the file wasn't found in DB. This can happen if the target was removed (cross-project duplications) or
   * if the target refers to an unchanged file in SLBs/PRs.
   */
  @CheckForNull
  public ComponentDto componentDto() {
    return componentDto;
  }
}
