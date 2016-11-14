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
package org.sonar.core.config;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.database.DatabaseProperties;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;

public class CorePropertyDefinitionsTest {
  @Test
  public void all() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();
    assertThat(defs).hasSize(65);
  }

  @Test
  public void jdbc_password_property_has_password_type() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();
    Optional<PropertyDefinition> prop = from(defs).filter(new HasKeyPredicate(DatabaseProperties.PROP_PASSWORD)).first();
    assertThat(prop.get().type()).isEqualTo(PropertyType.PASSWORD);
  }

  private final class HasKeyPredicate implements Predicate<PropertyDefinition> {
    private final String key;

    HasKeyPredicate(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nonnull PropertyDefinition input) {
      return key.equals(input.key());
    }
  }
}
