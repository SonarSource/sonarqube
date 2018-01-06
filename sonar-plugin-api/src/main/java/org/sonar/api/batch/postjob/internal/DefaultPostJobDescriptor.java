/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.batch.postjob.internal;

import java.util.Arrays;
import java.util.Collection;
import org.sonar.api.batch.postjob.PostJobDescriptor;

public class DefaultPostJobDescriptor implements PostJobDescriptor {

  private String name;
  private String[] properties = new String[0];

  public String name() {
    return name;
  }

  public Collection<String> properties() {
    return Arrays.asList(properties);
  }

  @Override
  public DefaultPostJobDescriptor name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public DefaultPostJobDescriptor requireProperty(String... propertyKey) {
    return requireProperties(propertyKey);
  }

  @Override
  public DefaultPostJobDescriptor requireProperties(String... propertyKeys) {
    this.properties = propertyKeys;
    return this;
  }

}
