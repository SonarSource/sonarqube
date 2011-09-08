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

import java.util.Arrays;
import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.cpd.decorators.DuplicationDensityDecorator;
import org.sonar.plugins.cpd.decorators.SumDuplicationsDecorator;

@Properties({
    @Property(
        key = CoreProperties.CPD_ENGINE,
        defaultValue = CoreProperties.CPD_ENGINE_DEFAULT_VALUE,
        name = "Engine",
        description = "Engine for detection of duplications. Possible values: sonar, pmd.",
        project = true,
        module = true,
        global = true,
        category = CoreProperties.CATEGORY_DUPLICATIONS),
    @Property(
        key = CoreProperties.CPD_CROSS_RPOJECT,
        defaultValue = CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE + "",
        name = "(Sonar) Detection across projects",
        description = "",
        project = true,
        module = true,
        global = true,
        category = CoreProperties.CATEGORY_DUPLICATIONS),
    @Property(
        key = CoreProperties.CPD_MINIMUM_TOKENS_PROPERTY,
        defaultValue = CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE + "",
        name = "(PMD) Minimum tokens",
        description = "The number of duplicate tokens above which a block is considered as a duplication.",
        project = true,
        module = true,
        global = true,
        category = CoreProperties.CATEGORY_DUPLICATIONS),
    @Property(
        key = CoreProperties.CPD_IGNORE_LITERALS_PROPERTY,
        defaultValue = CoreProperties.CPD_IGNORE_LITERALS_DEFAULT_VALUE + "",
        name = "(PMD) Ignore literals",
        description = "if true, PMD-CPD ignores literal value differences when evaluating a duplicate block. " +
            "This means that foo=\"first string\"; and foo=\"second string\"; will be seen as equivalent.",
        project = true,
        module = true,
        global = true,
        category = CoreProperties.CATEGORY_DUPLICATIONS),
    @Property(
        key = CoreProperties.CPD_IGNORE_IDENTIFIERS_PROPERTY,
        defaultValue = CoreProperties.CPD_IGNORE_IDENTIFIERS_DEFAULT_VALUE + "",
        name = "(PMD) Ignore identifiers",
        description = "Similar to 'Ignore literals' but for identifiers; i.e., variable names, methods names, and so forth.",
        project = true,
        module = true,
        global = true,
        category = CoreProperties.CATEGORY_DUPLICATIONS)
})
public class CpdPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(CpdSensor.class, SumDuplicationsDecorator.class, DuplicationDensityDecorator.class, JavaCpdMapping.class, SonarEngine.class, PmdEngine.class);
  }

}
