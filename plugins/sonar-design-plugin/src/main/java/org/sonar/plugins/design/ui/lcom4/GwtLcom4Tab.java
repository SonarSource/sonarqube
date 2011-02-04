/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.design.ui.lcom4;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Resource;
import org.sonar.api.web.*;
import org.sonar.plugins.design.ui.lcom4.client.Lcom4Tab;

@ResourceLanguage(Java.KEY)
@ResourceQualifier({Resource.QUALIFIER_CLASS})
@DefaultTab(metrics = {CoreMetrics.LCOM4_KEY})
@NavigationSection({NavigationSection.RESOURCE_TAB})
@UserRole(UserRole.CODEVIEWER)
public class GwtLcom4Tab extends GwtPage {

  public String getTitle() {
    return "LCOM4";
  }

  public String getGwtId() {
    return Lcom4Tab.GWT_ID;
  }
}
