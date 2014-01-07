/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.plugins.core.issue.ignore;

import com.google.common.collect.ImmutableList;
import org.sonar.plugins.core.issue.ignore.pattern.ExclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.InclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.scanner.RegexpScanner;
import org.sonar.plugins.core.issue.ignore.scanner.SourceScanner;

import java.util.List;

public final class IgnoreIssuesPlugin {

  private IgnoreIssuesPlugin() {
    // static extension declaration only
  }

  public static List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.addAll(IgnoreIssuesConfiguration.getPropertyDefinitions());
    extensions.add(
        InclusionPatternInitializer.class,
        ExclusionPatternInitializer.class,
        RegexpScanner.class,
        SourceScanner.class,
        EnforceIssuesFilter.class,
        IgnoreIssuesFilter.class);

    return extensions.build();
  }

}
