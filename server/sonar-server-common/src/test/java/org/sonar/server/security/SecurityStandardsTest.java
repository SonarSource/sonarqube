/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.security;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.server.security.SecurityStandards.SQCategory;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.security.SecurityStandards.CWES_BY_SQ_CATEGORY;
import static org.sonar.server.security.SecurityStandards.SQ_CATEGORY_KEYS_ORDERING;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;

public class SecurityStandardsTest {
  @Test
  public void fromSecurityStandards_from_empty_set_has_SQCategory_OTHERS() {
    SecurityStandards securityStandards = fromSecurityStandards(emptySet());

    assertThat(securityStandards.getStandards()).isEmpty();
    assertThat(securityStandards.getSqCategory()).isEqualTo(SQCategory.OTHERS);
    assertThat(securityStandards.getIgnoredSQCategories()).isEmpty();
  }

  @Test
  public void fromSecurityStandards_from_empty_set_has_unkwown_cwe_standard() {
    SecurityStandards securityStandards = fromSecurityStandards(emptySet());

    assertThat(securityStandards.getStandards()).isEmpty();
    assertThat(securityStandards.getCwe()).containsOnly("unknown");
  }

  @Test
  public void fromSecurityStandards_from_empty_set_has_no_OwaspTop10_standard() {
    SecurityStandards securityStandards = fromSecurityStandards(emptySet());

    assertThat(securityStandards.getStandards()).isEmpty();
    assertThat(securityStandards.getOwaspTop10()).isEmpty();
  }

  @Test
  public void fromSecurityStandards_from_empty_set_has_no_SansTop25_standard() {
    SecurityStandards securityStandards = fromSecurityStandards(emptySet());

    assertThat(securityStandards.getStandards()).isEmpty();
    assertThat(securityStandards.getSansTop25()).isEmpty();
  }

  @Test
  public void fromSecurityStandards_finds_SQCategory_from_any_if_the_mapped_CWE_standard() {
    CWES_BY_SQ_CATEGORY.forEach((sqCategory, cwes) -> {
      cwes.forEach(cwe -> {
        SecurityStandards securityStandards = fromSecurityStandards(singleton("cwe:" + cwe));

        assertThat(securityStandards.getSqCategory()).isEqualTo(sqCategory);
      });
    });
  }

  @Test
  public void fromSecurityStandards_finds_SQCategory_from_multiple_of_the_mapped_CWE_standard() {
    CWES_BY_SQ_CATEGORY.forEach((sqCategory, cwes) -> {
      SecurityStandards securityStandards = fromSecurityStandards(cwes.stream().map(t -> "cwe:" + t).collect(toSet()));

      assertThat(securityStandards.getSqCategory()).isEqualTo(sqCategory);
    });
  }

  @Test
  public void fromSecurityStandards_finds_SQCategory_first_in_order_when_CWEs_map_to_multiple_SQCategories() {
    EnumSet<SQCategory> sqCategories = EnumSet.allOf(SQCategory.class);
    sqCategories.remove(SQCategory.OTHERS);

    while (!sqCategories.isEmpty()) {
      SQCategory expected = sqCategories.stream().min(SQ_CATEGORY_KEYS_ORDERING.onResultOf(SQCategory::getKey)).get();
      SQCategory[] expectedIgnored = sqCategories.stream().filter(t -> t != expected).toArray(SQCategory[]::new);

      Set<String> cwes = sqCategories.stream()
        .flatMap(t -> CWES_BY_SQ_CATEGORY.get(t).stream().map(e -> "cwe:" + e))
        .collect(Collectors.toSet());
      SecurityStandards securityStandards = fromSecurityStandards(cwes);

      assertThat(securityStandards.getSqCategory()).isEqualTo(expected);
      assertThat(securityStandards.getIgnoredSQCategories()).containsOnly(expectedIgnored);

      sqCategories.remove(expected);
    }
  }
}
