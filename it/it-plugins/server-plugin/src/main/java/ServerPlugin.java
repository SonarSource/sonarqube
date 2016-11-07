
/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.util.Arrays;
import java.util.List;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;

@Properties({
  @Property(key = "some-property", name = "Some Property", defaultValue = "aDefaultValue", global = true, project = false),
  @Property(key = "boolean", name = "Boolean", defaultValue = "true", type = PropertyType.BOOLEAN, global = true, project = false),
  @Property(key = "user", name = "User", type = PropertyType.USER_LOGIN, global = true, project = false),
  @Property(key = "list", name = "List", type = PropertyType.SINGLE_SELECT_LIST, options = {"A", "B", "C"}, global = true, project = false),
  @Property(key = "metric", name = "Metric", type = PropertyType.METRIC, global = true, project = false),
  @Property(key = "metric_level", name = "Metric Level", type = PropertyType.METRIC_LEVEL, global = true, project = false),
  @Property(key = "float", name = "Float", type = PropertyType.FLOAT, global = true, project = false),
  @Property(key = "int", name = "Integer", type = PropertyType.INTEGER, global = true, project = false),
  @Property(key = "string", name = "String", type = PropertyType.STRING, global = true, project = false),
  @Property(key = "setting.license", name = "License", type = PropertyType.LICENSE, global = true, project = false),
  @Property(key = "long", name = "Long", type = PropertyType.LONG, global = true, project = false),
  @Property(key = "password", name = "Password", type = PropertyType.PASSWORD, global = true, project = false),
  @Property(key = "text", name = "Text", type = PropertyType.TEXT, global = true, project = false),
  @Property(key = "multi", name = "Multi", type = PropertyType.STRING, multiValues = true, global = true, project = false),
  @Property(key = "hidden", name = "Hidden", type = PropertyType.STRING, global = false, project = false),
  @Property(key = "setting.secured", name = "Secured", type = PropertyType.STRING, global = true, project = false)
})
public class ServerPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(
      StartupCrash.class, TempFolderExtension.class);
  }
}
