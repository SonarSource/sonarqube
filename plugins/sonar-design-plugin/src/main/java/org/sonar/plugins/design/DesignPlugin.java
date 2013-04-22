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
package org.sonar.plugins.design;

import com.google.common.collect.ImmutableList;
import org.sonar.api.Extension;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.design.batch.FileTangleIndexDecorator;
import org.sonar.plugins.design.batch.MavenDependenciesSensor;
import org.sonar.plugins.design.batch.PackageTangleIndexDecorator;
import org.sonar.plugins.design.batch.ProjectDsmDecorator;
import org.sonar.plugins.design.batch.SuspectLcom4DensityDecorator;
import org.sonar.plugins.design.ui.dependencies.DependenciesViewer;
import org.sonar.plugins.design.ui.libraries.GwtLibrariesPage;
import org.sonar.plugins.design.ui.page.GwtDesignPage;
import org.sonar.plugins.design.ui.widgets.FileDesignWidget;
import org.sonar.plugins.design.ui.widgets.LCOM4Widget;
import org.sonar.plugins.design.ui.widgets.PackageDesignWidget;
import org.sonar.plugins.design.ui.widgets.ResponseForClassWidget;

import java.util.List;

public class DesignPlugin extends SonarPlugin {

  @SuppressWarnings("unchecked")
  public List<Class<? extends Extension>> getExtensions() {
    return ImmutableList.of(
        // Batch
        MavenDependenciesSensor.class,
        ProjectDsmDecorator.class,
        PackageTangleIndexDecorator.class,
        FileTangleIndexDecorator.class,
        SuspectLcom4DensityDecorator.class,
        GwtLibrariesPage.class,

        // UI
        GwtDesignPage.class,
        DependenciesViewer.class,
        FileDesignWidget.class,
        PackageDesignWidget.class,
        LCOM4Widget.class,
        ResponseForClassWidget.class);
  }
}
