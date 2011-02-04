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
package org.sonar.plugins.design;

import org.sonar.api.*;
import org.sonar.plugins.design.batch.*;
import org.sonar.plugins.design.ui.dependencies.GwtDependenciesTab;
import org.sonar.plugins.design.ui.lcom4.GwtLcom4Tab;
import org.sonar.plugins.design.ui.libraries.GwtLibrariesPage;
import org.sonar.plugins.design.ui.page.GwtDesignPage;
import org.sonar.plugins.design.ui.widgets.ChidamberKemererWidget;
import org.sonar.plugins.design.ui.widgets.FileDesignWidget;
import org.sonar.plugins.design.ui.widgets.PackageDesignWidget;

import java.util.ArrayList;
import java.util.List;

@Properties({
    @Property(
        key = CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY,
        defaultValue = "" + CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE,
        name = "Skip design analysis",
        project = true,
        global = true)
})
public class DesignPlugin implements Plugin {

  public String getKey() {
    return "design";
  }

  public String getName() {
    return "Design";
  }

  public String getDescription() {
    return "";
  }

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> extensions = new ArrayList<Class<? extends Extension>>();

    // Batch
    extensions.add(MavenDependenciesSensor.class);
    extensions.add(ProjectDsmDecorator.class);
    extensions.add(PackageTangleIndexDecorator.class);
    extensions.add(FileTangleIndexDecorator.class);
    extensions.add(SuspectLcom4DensityDecorator.class);
    extensions.add(GwtLibrariesPage.class);

    // UI
    extensions.add(GwtDesignPage.class);
    extensions.add(GwtDependenciesTab.class);
    extensions.add(FileDesignWidget.class);
    extensions.add(PackageDesignWidget.class);
    extensions.add(ChidamberKemererWidget.class);
    extensions.add(GwtLcom4Tab.class);

    return extensions;
  }

  @Override
  public String toString() {
    return getKey();
  }
}
