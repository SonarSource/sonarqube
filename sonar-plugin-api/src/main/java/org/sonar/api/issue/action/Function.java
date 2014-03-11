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
package org.sonar.api.issue.action;

import com.google.common.annotations.Beta;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;

import javax.annotation.Nullable;

/**
 * @since 3.6
 */
@Beta
public interface Function {

  void execute(Context context);

  interface Context {
    Issue issue();

    Settings projectSettings();

    Context setAttribute(String key, @Nullable String value);

    Context addComment(@Nullable String text);
  }

}
