/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import ce.CePauseStep;
import ce.PauseMetric;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyField;
import org.sonar.api.SonarPlugin;

import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.FLOAT;
import static org.sonar.api.PropertyType.INTEGER;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.api.PropertyType.LONG;
import static org.sonar.api.PropertyType.METRIC;
import static org.sonar.api.PropertyType.METRIC_LEVEL;
import static org.sonar.api.PropertyType.PASSWORD;
import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.api.PropertyType.SINGLE_SELECT_LIST;
import static org.sonar.api.PropertyType.STRING;
import static org.sonar.api.PropertyType.TEXT;
import static org.sonar.api.PropertyType.USER_LOGIN;

@Properties({
  @Property(key = "some-property", name = "Some Property", defaultValue = "aDefaultValue", global = true, project = false),
  @Property(key = "boolean", name = "Boolean", defaultValue = "true", type = BOOLEAN, global = true, project = false),
  @Property(key = "user", name = "User", type = USER_LOGIN, global = true, project = false),
  @Property(key = "list", name = "List", type = SINGLE_SELECT_LIST, options = {"A", "B", "C"}, global = true, project = false),
  @Property(key = "metric", name = "Metric", type = METRIC, global = true, project = false),
  @Property(key = "metric_level", name = "Metric Level", type = METRIC_LEVEL, global = true, project = false),
  @Property(key = "float", name = "Float", type = FLOAT, global = true, project = false),
  @Property(key = "int", name = "Integer", type = INTEGER, global = true, project = false),
  @Property(key = "string", name = "String", type = STRING, global = true, project = false),
  @Property(key = "setting.license.secured", name = "License", type = LICENSE, global = true, project = false),
  @Property(key = "long", name = "Long", type = LONG, global = true, project = false),
  @Property(key = "password", name = "Password", type = PASSWORD, global = true, project = false),
  @Property(key = "text", name = "Text", type = TEXT, global = true, project = false),
  @Property(key = "multi", name = "Multi", type = STRING, multiValues = true, global = true, project = false),
  @Property(key = "hidden", name = "Hidden", type = STRING, global = false, project = false),
  @Property(key = "project.setting", name = "Project setting", type = STRING, global = false, project = true),
  @Property(key = "setting.secured", name = "Secured", type = STRING, global = true, project = false),
  @Property(key = "sonar.jira", name = "Jira Server", type = PROPERTY_SET, propertySetKey = "jira", fields = {
    @PropertyField(key = "key", name = "Key", description = "Server key"),
    @PropertyField(key = "type", name = "Type", options = {"A", "B"}),
    @PropertyField(key = "url", name = "URL"),
    @PropertyField(key = "port", name = "Port", type = INTEGER)}),
})
public class ServerPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(
      StartupCrash.class, ServerStartupLock.class, TempFolderExtension.class, PauseMetric.class, CePauseStep.class);
  }
}
