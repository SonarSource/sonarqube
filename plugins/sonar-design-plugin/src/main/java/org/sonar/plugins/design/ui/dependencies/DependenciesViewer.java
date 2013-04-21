/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.design.ui.dependencies;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.DefaultTab;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RequiredMeasures;
import org.sonar.api.web.ResourceQualifier;
import org.sonar.api.web.RubyRailsPage;
import org.sonar.api.web.UserRole;

@RequiredMeasures(allOf = {CoreMetrics.AFFERENT_COUPLINGS_KEY, CoreMetrics.EFFERENT_COUPLINGS_KEY})
@ResourceQualifier({Qualifiers.FILE, Qualifiers.CLASS, Qualifiers.PACKAGE, Qualifiers.PROJECT, Qualifiers.MODULE})
@DefaultTab(metrics = {CoreMetrics.AFFERENT_COUPLINGS_KEY, CoreMetrics.EFFERENT_COUPLINGS_KEY})
@NavigationSection({NavigationSection.RESOURCE_TAB})
@UserRole(UserRole.CODEVIEWER)
public class DependenciesViewer extends AbstractRubyTemplate implements RubyRailsPage {

  public String getId() {
    return "dependencies";
  }

  public String getTitle() {
    return "Dependencies";
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/plugins/design/ui/dependencies/dependencies_viewer.html.erb";
  }
}
