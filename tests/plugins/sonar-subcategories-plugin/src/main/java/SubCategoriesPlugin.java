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
import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class SubCategoriesPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(
        PropertyDefinition.builder("prop1")
            .index(2)
            .category("Category 1")
            .subCategory("Sub category 1")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop2")
            .index(1)
            // SONAR-4501 category are case insensitive
            .category("category 1")
            .subCategory("Sub category 1")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop3")
            .category("Category 1")
            .subCategory("Sub category 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop5")
            .category("Category 1")
            // SONAR-4501 subcategory are case insensitive
            .subCategory("sub category 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop4")
            .category("Category 1")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop2_1")
            .category("Category 2")
            .subCategory("Sub category 1 of 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop2_2")
            .category("Category 2")
            .subCategory("Sub category 2 of 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop_only_on_project")
            .category("project-only")
            .description("Foo")
            .onlyOnQualifiers(Qualifiers.PROJECT)
            .build());
  }
}
