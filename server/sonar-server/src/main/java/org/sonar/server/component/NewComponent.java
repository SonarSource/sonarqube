/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;

import static com.google.common.base.Preconditions.checkArgument;

public class NewComponent {

  private static final int MAX_KEY_LENGHT = 400;
  private static final int MAX_NAME_LENGTH = 2000;
  private static final int MAX_QUALIFIER_LENGTH = 10;
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
    if (qualifier != null) {
      checkArgument(qualifier.length() <= MAX_QUALIFIER_LENGTH,
        "Component qualifier length (%s) is longer than the maximum authorized (%s)", qualifier.length(), MAX_QUALIFIER_LENGTH);
    }

    this.qualifier = qualifier;
    return this;
  }

  public static NewComponent create(String key, String name) {
    checkArgument(key != null, "Key can't be null");
    checkArgument(key.length() <= MAX_KEY_LENGHT, "Component key length (%s) is longer than the maximum authorized (%s)", key.length(), MAX_KEY_LENGHT);
    checkArgument(name != null, "Name can't be null");
    checkArgument(name.length() <= MAX_NAME_LENGTH, "Component name length (%s) is longer than the maximum authorized (%s)", name.length(), MAX_NAME_LENGTH);
    return new NewComponent(key, name);
  }
}
