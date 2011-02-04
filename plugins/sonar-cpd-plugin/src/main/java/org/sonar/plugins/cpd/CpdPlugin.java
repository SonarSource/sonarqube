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
package org.sonar.plugins.cpd;

import org.sonar.api.*;
import org.sonar.plugins.cpd.decorators.DuplicationDensityDecorator;
import org.sonar.plugins.cpd.decorators.SumDuplicationsDecorator;

import java.util.ArrayList;
import java.util.List;

@Properties({
    @Property(
        key = CoreProperties.CPD_MINIMUM_TOKENS_PROPERTY,
        defaultValue = CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE + "",
        name = "Minimum tokens",
        description = "The number of duplicate tokens above which a block is considered as a duplication.",
        project = true,
        module = true,
        global = true),
    @Property(
            key = CoreProperties.CPD_IGNORE_LITERALS_PROPERTY,
            defaultValue = CoreProperties.CPD_IGNORE_LITERALS_DEFAULT_VALUE + "",
            name = "Ignore literals",
            description = "if true, CPD ignores literal value differences when evaluating a duplicate block. " +
            		"This means that foo=\"first string\"; and foo=\"second string\"; will be seen as equivalent.",
            project = true,
            module = true,
            global = true),
    @Property(
        key = CoreProperties.CPD_IGNORE_IDENTIFIERS_PROPERTY,
        defaultValue = CoreProperties.CPD_IGNORE_IDENTIFIERS_DEFAULT_VALUE + "",
        name = "Ignore identifiers",
        description = "Similar to 'Ignore literals' but for identifiers; i.e., variable names, methods names, and so forth.",
        project = true,
        module = true,
        global = true),
    @Property(
        key = CoreProperties.CPD_SKIP_PROPERTY,
        defaultValue = "false",
        name = "Skip detection of duplicated code",
        description = "Searching for duplicated code is memory hungry therefore for very big projects it can be necessary to turn the functionality off.",
        project = true,
        module = true,
        global = true)
})
public class CpdPlugin implements Plugin {

  public String getKey() {
    return CoreProperties.CPD_PLUGIN;
  }

  public String getName() {
    return "Duplications";
  }

  public String getDescription() {
    return "Find duplicated source code within project.";
  }

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();
    list.add(CpdSensor.class);
    list.add(SumDuplicationsDecorator.class);
    list.add(DuplicationDensityDecorator.class);
    list.add(JavaCpdMapping.class);
    return list;
  }
}