/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition.OwaspAsvsVersion;
import org.sonar.server.security.SecurityStandards.OwaspAsvs;
import org.sonar.server.security.SecurityStandards.PciDss;
import org.sonar.server.security.SecurityStandards.SQCategory;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sonar.server.security.SecurityStandards.CWES_BY_SQ_CATEGORY;
import static org.sonar.server.security.SecurityStandards.OWASP_ASVS_REQUIREMENTS_BY_LEVEL;
import static org.sonar.server.security.SecurityStandards.SQ_CATEGORY_KEYS_ORDERING;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;
import static org.sonar.server.security.SecurityStandards.getRequirementsForCategoryAndLevel;

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
  public void fromSecurityStandards_from_empty_set_has_no_CweTop25_standard() {
    SecurityStandards securityStandards = fromSecurityStandards(emptySet());

    assertThat(securityStandards.getStandards()).isEmpty();
    assertThat(securityStandards.getCweTop25()).isEmpty();
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

  @Test
  public void pciDss_categories_check() {
    List<String> pciDssCategories = Arrays.stream(PciDss.values()).map(PciDss::category).toList();

    assertThat(pciDssCategories).hasSize(12).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
  }

  @Test
  public void owaspAsvs_categories_check() {
    List<String> owaspAsvsCategories = Arrays.stream(OwaspAsvs.values()).map(OwaspAsvs::category).toList();

    assertThat(owaspAsvsCategories).hasSize(14).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14");
  }

  @Test
  public void owaspAsvs40_requirements_distribution_by_level_check() {
    assertTrue(OWASP_ASVS_REQUIREMENTS_BY_LEVEL.containsKey(OwaspAsvsVersion.V4_0));
    assertTrue(OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(OwaspAsvsVersion.V4_0).containsKey(1));
    assertTrue(OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(OwaspAsvsVersion.V4_0).containsKey(2));
    assertTrue(OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(OwaspAsvsVersion.V4_0).containsKey(3));
    assertEquals(135, OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(OwaspAsvsVersion.V4_0).get(1).size());
    assertEquals(266, OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(OwaspAsvsVersion.V4_0).get(2).size());
    assertEquals(286, OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(OwaspAsvsVersion.V4_0).get(3).size());
  }

  @Test
  public void owaspAsvs40_requirements_by_category_and_level_check() {
    assertEquals(0, getRequirementsForCategoryAndLevel(OwaspAsvs.C1, 1).size());
    assertEquals(31, getRequirementsForCategoryAndLevel(OwaspAsvs.C2, 1).size());
    assertEquals(12, getRequirementsForCategoryAndLevel(OwaspAsvs.C3, 1).size());
    assertEquals(9, getRequirementsForCategoryAndLevel(OwaspAsvs.C4, 1).size());
    assertEquals(27, getRequirementsForCategoryAndLevel(OwaspAsvs.C5, 1).size());
    assertEquals(1, getRequirementsForCategoryAndLevel(OwaspAsvs.C6, 1).size());
    assertEquals(3, getRequirementsForCategoryAndLevel(OwaspAsvs.C7, 1).size());
    assertEquals(7, getRequirementsForCategoryAndLevel(OwaspAsvs.C8, 1).size());
    assertEquals(3, getRequirementsForCategoryAndLevel(OwaspAsvs.C9, 1).size());
    assertEquals(3, getRequirementsForCategoryAndLevel(OwaspAsvs.C10, 1).size());
    assertEquals(5, getRequirementsForCategoryAndLevel(OwaspAsvs.C11, 1).size());
    assertEquals(11, getRequirementsForCategoryAndLevel(OwaspAsvs.C12, 1).size());
    assertEquals(7, getRequirementsForCategoryAndLevel(OwaspAsvs.C13, 1).size());
    assertEquals(16, getRequirementsForCategoryAndLevel(OwaspAsvs.C14, 1).size());
  }
}
