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

import com.google.common.collect.ImmutableList;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ActiveRule {

  public enum Inheritance {
    NONE, OVERRIDES, INHERITED;
    public static final List<Inheritance> ALL = ImmutableList.of(NONE, OVERRIDES, INHERITED);
  }

  Date createdAt();

  Date updatedAt();

  ActiveRuleKey key();

  String severity();

  Inheritance inheritance();

  @CheckForNull
  ActiveRuleKey parentKey();

  Map<String, String> params();

}
