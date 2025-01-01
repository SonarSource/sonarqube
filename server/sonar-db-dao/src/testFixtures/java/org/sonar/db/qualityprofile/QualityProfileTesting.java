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
package org.sonar.db.qualityprofile;

import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Consumer;
import org.sonar.core.util.Uuids;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.RandomStringUtils.secure;

public class QualityProfileTesting {

  private static final Random RANDOM = new SecureRandom();

  private QualityProfileTesting() {
    // prevent instantiation
  }

  /**
   * Create an instance of {@link  QProfileDto} with random field values.
   */
  public static QProfileDto newQualityProfileDto() {
    String uuid = Uuids.createFast();
    return new QProfileDto()
      .setKee(uuid)
      .setRulesProfileUuid(Uuids.createFast())
      .setName(uuid)
      .setLanguage(secure().nextAlphanumeric(20))
      .setLastUsed(RANDOM.nextLong(Long.MAX_VALUE));
  }

  /**
   * Create an instance of {@link  QProfileChangeDto} with random field values,
   * except changeType which is always {@code "ACTIVATED"}.
   */
  public static QProfileChangeDto newQProfileChangeDto() {
    return new QProfileChangeDto()
      .setUuid(secure().nextAlphanumeric(40))
      .setRulesProfileUuid(secure().nextAlphanumeric(40))
      .setCreatedAt(RANDOM.nextLong(Long.MAX_VALUE))
      .setChangeType("ACTIVATED")
      .setUserUuid("userUuid_" + secure().nextAlphanumeric(10));
  }

  /**
   * Create an instance of {@link  RulesProfileDto} with most of random field values.
   */
  public static RulesProfileDto newRuleProfileDto(Consumer<RulesProfileDto>... populators) {
    RulesProfileDto dto = new RulesProfileDto()
      .setUuid("uuid" + secure().nextAlphabetic(10))
      .setName("name" + secure().nextAlphabetic(10))
      .setLanguage("lang" + secure().nextAlphabetic(5))
      .setIsBuiltIn(false);
    stream(populators).forEach(p -> p.accept(dto));
    return dto;
  }
}
