/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.duplicationsviewer;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.api.web.*;
import org.sonar.plugins.core.duplicationsviewer.client.DuplicationsViewer;

@ResourceQualifier({Resource.QUALIFIER_CLASS,Resource.QUALIFIER_FILE})
@NavigationSection(NavigationSection.RESOURCE_TAB)
@DefaultTab(metrics={CoreMetrics.DUPLICATED_LINES_KEY, CoreMetrics.DUPLICATED_BLOCKS_KEY, CoreMetrics.DUPLICATED_FILES_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY})
@UserRole(UserRole.CODEVIEWER)
public class DuplicationsViewerDefinition extends GwtPage {

  public String getTitle() {
    return "Duplications";
  }

  public String getGwtId() {
    return DuplicationsViewer.GWT_ID;
  }

}
