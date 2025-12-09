/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user.ws;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.user.ws.HomepageTypes.Type.PROJECTS;

public class HomepageTypesImpl implements HomepageTypes {

  private List<Type> types;

  public HomepageTypesImpl() {
    types = Stream.of(HomepageTypes.Type.values()).toList();
  }

  @Override
  public List<Type> getTypes() {
    checkState(types != null, "Homepage types have not been initialized yet");
    return types;
  }

  @Override
  public Type getDefaultType() {
    return PROJECTS;
  }

}
