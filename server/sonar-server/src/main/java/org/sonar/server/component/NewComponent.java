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

package org.sonar.server.component;

import com.google.common.base.Preconditions;
import org.sonar.api.resources.Qualifiers;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class NewComponent {

  private String key;
  private String branch;
  private String qualifier;
  private String name;

  public NewComponent(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public String key() {
    return key;
  }

  public String name() {
    return name;
  }

  @CheckForNull
  public String branch() {
    return branch;
  }

  public NewComponent setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }

  public String qualifier() {
    return qualifier != null ? qualifier : Qualifiers.PROJECT;
  }

  public NewComponent setQualifier(@Nullable String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public static NewComponent create(String key, String name) {
    Preconditions.checkNotNull(key, "Key can't be null");
    Preconditions.checkNotNull(name, "Name can't be null");
    return new NewComponent(key, name);
  }
}
