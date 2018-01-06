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
import java.util.Collections;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;

@Properties({
    @Property(
        key = "untyped.license.secured",
        name = "Property without license type",
        category = CoreProperties.CATEGORY_GENERAL),
    @Property(
        key = "typed.license.secured",
        name = "Typed property",
        category = CoreProperties.CATEGORY_GENERAL,
        type = PropertyType.LICENSE)
})
public class LicensePlugin extends SonarPlugin {
  public List getExtensions() {
    return Collections.emptyList();
  }
}
