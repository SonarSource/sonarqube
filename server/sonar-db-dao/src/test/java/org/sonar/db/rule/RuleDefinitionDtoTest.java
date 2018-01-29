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

package org.sonar.db.rule;

import com.google.common.collect.ImmutableSet;
import java.util.Random;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleDefinitionDtoTest {

  private static final Random RANDOM = new Random();

  @Test
  public void test_equality() {
    String repositoryKey = randomAlphanumeric(10);
    String ruleKey = randomAlphanumeric(10);
    RuleDefinitionDto underTest = new RuleDefinitionDto().setRepositoryKey(repositoryKey).setRuleKey(ruleKey);

    // Comparison is done only with repository key and rule key
    assertThat(underTest).isEqualTo(new RuleDefinitionDto()
      .setRepositoryKey(repositoryKey)
      .setRuleKey(ruleKey));

    // All other fields are ignored
    assertThat(underTest).isEqualTo(new RuleDefinitionDto()
      .setRepositoryKey(repositoryKey)
      .setRuleKey(ruleKey)
      .setLanguage(randomAlphanumeric(5))
      .setUpdatedAt(RANDOM.nextInt())
      .setCreatedAt(RANDOM.nextInt())
      .setName(randomAlphanumeric(10))
      .setSeverity(RANDOM.nextInt())
      .setType(RANDOM.nextInt(3))
      .setSystemTags(ImmutableSet.of("test", "test2"))
      .setDescription(randomAlphanumeric(50))
      .setIsTemplate(RANDOM.nextBoolean())
      .setId(RANDOM.nextInt())
      .setTemplateId(RANDOM.nextInt())
      .setDescriptionFormat(RANDOM.nextBoolean() ? RuleDto.Format.HTML : RuleDto.Format.MARKDOWN)
      .setDefRemediationBaseEffort(randomAlphanumeric(10))
      .setDefRemediationFunction(randomAlphanumeric(10))
      .setDefRemediationGapMultiplier(randomAlphanumeric(10))
      .setGapDescription(randomAlphanumeric(50))
    );

    // Must not be equal to other rule with other rule key
    assertThat(underTest).isNotEqualTo(new RuleDefinitionDto()
      .setRepositoryKey(repositoryKey)
      .setRuleKey(randomAlphanumeric(9))
    );

    // Comparison is done only with repository key and repository key
    assertThat(underTest).isNotEqualTo(new RuleDefinitionDto()
      .setRepositoryKey(randomAlphanumeric(9))
      .setRuleKey(ruleKey)
    );
  }
}
