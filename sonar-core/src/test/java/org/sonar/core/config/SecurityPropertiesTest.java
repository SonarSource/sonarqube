/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.core.config;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinition;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityPropertiesTest {

  @Test
  public void creates_properties() {
    assertThat(SecurityProperties.all()).isNotEmpty();
    Optional<PropertyDefinition> propertyDefinition = SecurityProperties.all().stream()
      .filter(d -> d.key().equals("sonar.forceAuthentication")).findFirst();
    assertThat(propertyDefinition)
      .isNotEmpty()
      .get()
      .extracting(PropertyDefinition::defaultValue)
      .isEqualTo("true");
  }

  @Test
  public void creates_secret_source_redaction_property_disabled_by_default() {
    Optional<PropertyDefinition> propertyDefinition = SecurityProperties.all().stream()
      .filter(d -> d.key().equals(SecurityProperties.SECRET_SOURCE_REDACTION_ENABLED)).findFirst();

    assertThat(propertyDefinition)
      .isNotEmpty()
      .get()
      .extracting(PropertyDefinition::defaultValue)
      .isEqualTo("false");
  }

  @Test
  public void webhooks_allowed_addresses_property_is_displayed_right_after_validate_webhooks_property() {
    List<String> displayOrder = SecurityProperties.all().stream()
      .filter(d -> d.subCategory().equals(SecurityProperties.SUBCATEGORY_WEBHOOKS))
      .sorted(comparing(PropertyDefinition::index).thenComparing(PropertyDefinition::name, String::compareToIgnoreCase))
      .map(PropertyDefinition::key)
      .toList();

    int validateWebhooksPosition = displayOrder.indexOf(CoreProperties.SONAR_VALIDATE_WEBHOOKS_PROPERTY);
    assertThat(displayOrder.get(validateWebhooksPosition + 1)).isEqualTo(SecurityProperties.WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY);
  }

  @Test
  public void creates_webhooks_allowed_addresses_property_as_empty_multi_valued_security_setting() {
    Optional<PropertyDefinition> propertyDefinition = SecurityProperties.all().stream()
      .filter(d -> d.key().equals(SecurityProperties.WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).findFirst();

    assertThat(propertyDefinition).isNotEmpty();
    PropertyDefinition definition = propertyDefinition.get();
    assertThat(definition.key()).isEqualTo("sonar.validateWebhooks.allowedAddresses");
    assertThat(definition.multiValues()).isTrue();
    assertThat(definition.defaultValue()).isEmpty();
    assertThat(definition.category()).isEqualTo("security");
    assertThat(definition.global()).isTrue();
  }

  @Test
  public void webhooks_properties_are_in_webhooks_subcategory() {
    assertThat(SecurityProperties.all())
      .filteredOn(d -> d.subCategory().equals(SecurityProperties.SUBCATEGORY_WEBHOOKS))
      .extracting(PropertyDefinition::key)
      .containsExactly(CoreProperties.SONAR_VALIDATE_WEBHOOKS_PROPERTY, SecurityProperties.WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY);
  }

}
