/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.design.ui.dependencies;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.api.web.DefaultTab;
import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RequiredMeasures;
import org.sonar.api.web.ResourceQualifier;
import org.sonar.api.web.UserRole;
import org.sonar.plugins.design.ui.dependencies.client.DependenciesTab;

@RequiredMeasures(mandatory = {CoreMetrics.AFFERENT_COUPLINGS_KEY, CoreMetrics.EFFERENT_COUPLINGS_KEY})
@ResourceQualifier({Resource.QUALIFIER_FILE, Resource.QUALIFIER_CLASS, Resource.QUALIFIER_PACKAGE, Resource.QUALIFIER_PROJECT, Resource.QUALIFIER_MODULE})
@DefaultTab(metrics = {CoreMetrics.AFFERENT_COUPLINGS_KEY, CoreMetrics.EFFERENT_COUPLINGS_KEY})
@NavigationSection({NavigationSection.RESOURCE_TAB})
@UserRole(UserRole.USER)
public class GwtDependenciesTab extends GwtPage {

  public String getTitle() {
    return "Dependencies";
  }

  public String getGwtId() {
    return DependenciesTab.GWT_ID;
  }
}
