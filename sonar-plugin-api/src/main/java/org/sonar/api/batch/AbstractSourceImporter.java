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
package org.sonar.api.batch;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/**
 * @since 1.10
 * @deprecated since 4.2 Component indexing and source import are done by core and this extension is not used.
 */
@Deprecated
@Phase(name = Phase.Name.PRE)
public abstract class AbstractSourceImporter implements Sensor {

  private Language language;

  public AbstractSourceImporter(Language language) {
    this.language = language;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return false;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    // Do not remove for backward compatibility
  }

  protected void onFinished() {

  }

  protected void parseDirs(SensorContext context, List<File> files, List<File> sourceDirs, boolean unitTest, Charset sourcesEncoding) {
    // Do not remove for backward compatibility
  }

  protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
    return null;
  }

  protected boolean isEnabled(Project project) {
    return false;
  }

  public Language getLanguage() {
    return language;
  }
}
