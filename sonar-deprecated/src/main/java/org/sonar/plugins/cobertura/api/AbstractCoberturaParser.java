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
package org.sonar.plugins.cobertura.api;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.CoberturaReportParserUtils;
import org.sonar.api.utils.CoberturaReportParserUtils.FileResolver;

import java.io.File;

/**
 * @since 2.4
 * @deprecated since 3.7 Used to keep backward compatibility since extraction
 * of Cobertura plugin.
 */
@Deprecated
public abstract class AbstractCoberturaParser {

  public void parseReport(File xmlFile, final SensorContext context) {
    CoberturaReportParserUtils.parseReport(xmlFile, context, new FileResolver() {
      @Override
      public Resource<?> resolve(String filename) {
        return getResource(filename);
      }
    });
  }

  protected abstract Resource getResource(String fileName);
}
