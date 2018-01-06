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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerExtension;

@Properties({
    @Property(key = "boolean", name = "Boolean", type=PropertyType.BOOLEAN),
    @Property(key = "integer", name = "Integer", type=PropertyType.INTEGER),
    @Property(key = "float", name = "Float", type=PropertyType.FLOAT),
    @Property(key = "password", name = "Password", type=PropertyType.PASSWORD, defaultValue = "sonar"),
    @Property(key = "text", name = "Text", type=PropertyType.TEXT),
    @Property(key = "metric", name = "Metric", type=PropertyType.METRIC),
    @Property(key = "single_select_list", name = "Single Select List", type=PropertyType.SINGLE_SELECT_LIST, options = {"de", "en", "nl"})
})
public class PropertyTypes implements ServerExtension {
}
