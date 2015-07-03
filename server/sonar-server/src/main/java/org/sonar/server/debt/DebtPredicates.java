/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.debt;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import javax.annotation.Nonnull;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.db.debt.CharacteristicDto;

public class DebtPredicates {

  private DebtPredicates() {
    // Only static stuff
  }

  public static class CharacteristicDtoMatchKey implements Predicate<CharacteristicDto> {
    private final String key;

    public CharacteristicDtoMatchKey(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nonnull CharacteristicDto input) {
      return input.getKey().equals(key);
    }
  }

  public static class CharacteristicDtoParentIdMatchId implements Predicate<CharacteristicDto> {
    private final Integer id;

    public CharacteristicDtoParentIdMatchId(Integer id) {
      this.id = id;
    }

    @Override
    public boolean apply(@Nonnull CharacteristicDto input) {
      return id.equals(input.getParentId());
    }
  }

  public static class DebtCharacteristicMatchKey implements Predicate<DebtCharacteristic> {
    private final String key;

    public DebtCharacteristicMatchKey(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nonnull DebtCharacteristic input) {
      return input.key().equals(key);
    }
  }

  public static class DebtCharacteristicMatchId implements Predicate<DebtCharacteristic> {
    private final int id;

    public DebtCharacteristicMatchId(int id) {
      this.id = id;
    }

    @Override
    public boolean apply(@Nonnull DebtCharacteristic input) {
      return id == ((DefaultDebtCharacteristic) input).id();
    }
  }

  public enum ToDebtCharacteristic implements Function<CharacteristicDto, DebtCharacteristic> {
    INSTANCE;

    @Override
    public DebtCharacteristic apply(@Nonnull CharacteristicDto input) {
      return toDebtCharacteristic(input);
    }
  }

  public static DebtCharacteristic toDebtCharacteristic(CharacteristicDto dto) {
    return new DefaultDebtCharacteristic()
      .setId(dto.getId())
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setOrder(dto.getOrder())
      .setParentId(dto.getParentId());
  }
}
