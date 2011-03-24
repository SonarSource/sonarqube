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
package org.sonar.plugins.cobertura;

import org.sonar.api.*;

import java.util.ArrayList;
import java.util.List;

@Properties({
    @Property(
        key = CoreProperties.COBERTURA_REPORT_PATH_PROPERTY,
        name = "Report path",
        description = "Path (absolute or relative) to Cobertura xml report file.",
        project = true,
        global = false),
    @Property(
        key = CoreProperties.COBERTURA_MAXMEM_PROPERTY,
        defaultValue = CoreProperties.COBERTURA_MAXMEM_DEFAULT_VALUE,
        name = "Maxmem",
        description = "Maximum memory to pass to JVM of Cobertura processes",
        project = true,
        global = true) })
public class CoberturaPlugin extends SonarPlugin {

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();
    list.add(CoberturaSensor.class);
    list.add(CoberturaMavenPluginHandler.class);
    list.add(CoberturaMavenInitializer.class);
    return list;
  }
}
