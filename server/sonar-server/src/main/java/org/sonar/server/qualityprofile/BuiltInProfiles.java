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

package org.sonar.server.qualityprofile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.sonar.api.ServerSide;

import java.util.Collection;

/**
 * Used to list default profile names.
 *
 * It should be removed as soon as a new API to define quality profiles is created. Currently loading all definitions
 * just to get the profile names is too slow (see {@link org.sonar.api.profiles.ProfileDefinition}).
 */
@ServerSide
public class BuiltInProfiles {

  // built-in profile names grouped by language
  private final Multimap<String, String> namesByLang;

  public BuiltInProfiles() {
    this.namesByLang = ArrayListMultimap.create();
  }

  Collection<String> byLanguage(String language) {
    return namesByLang.get(language);
  }

  BuiltInProfiles put(String language, String name) {
    namesByLang.put(language, name);
    return this;
  }

}
