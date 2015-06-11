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

package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.debt.CharacteristicImpl;
import org.sonar.server.computation.debt.MutableDebtModelHolder;
import org.sonar.server.db.DbClient;

/**
 * Populates the {@link org.sonar.server.computation.debt.DebtModelHolder}
 */
public class FeedDebtModelStep implements ComputationStep {

  private final DbClient dbClient;
  private final MutableDebtModelHolder mutableDebtModelHolder;
  private final Function<CharacteristicDto, Characteristic> dtoToCharFunction;
  private final Function<CharacteristicDto, Integer> dtoToParentIdFunction;
  private final Predicate<CharacteristicDto> isRootPredicate;

  public FeedDebtModelStep(DbClient dbClient, MutableDebtModelHolder mutableDebtModelHolder) {
    this.dbClient = dbClient;
    this.mutableDebtModelHolder = mutableDebtModelHolder;
    this.dtoToCharFunction = new Function<CharacteristicDto, Characteristic>() {
      @Nullable
      @Override
      public Characteristic apply(@Nonnull CharacteristicDto characteristicDto) {
        return toCharacteristic(characteristicDto);
      }
    };
    this.dtoToParentIdFunction = new Function<CharacteristicDto, Integer>() {
      @Nullable
      @Override
      public Integer apply(@Nonnull CharacteristicDto characteristicDto) {
        Integer parentId = characteristicDto.getParentId();
        return parentId != null ? parentId : null;
      }
    };
    this.isRootPredicate = new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(@Nonnull CharacteristicDto characteristicDto) {
        return characteristicDto.getParentId() == null;
      }
    };
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      feedDebtModel(session);
    } finally {
      session.close();
    }
  }

  private void feedDebtModel(DbSession session) {
    List<CharacteristicDto> characteristicDtos = dbClient.debtCharacteristicDao().selectEnabledCharacteristics(session);
    List<CharacteristicDto> rootCharacteristicDtos = FluentIterable.from(characteristicDtos)
      .filter(isRootPredicate).toList();
    Multimap<Integer, CharacteristicDto> subCharacteristicDtosByParentId = FluentIterable.from(characteristicDtos)
      .filter(Predicates.not(isRootPredicate))
      .index(dtoToParentIdFunction);
    for (CharacteristicDto rootCharacteristicDto : rootCharacteristicDtos) {
      Collection<CharacteristicDto> subCharacteristicDtos = subCharacteristicDtosByParentId.get(rootCharacteristicDto.getId());

      Characteristic rootCharacteristic = toCharacteristic(rootCharacteristicDto);
      List<Characteristic> subCharacteristic = FluentIterable.from(subCharacteristicDtos)
        .transform(dtoToCharFunction)
        .toList();
      mutableDebtModelHolder.addCharacteristics(rootCharacteristic, subCharacteristic);
    }
  }

  private static Characteristic toCharacteristic(CharacteristicDto dto) {
    return new CharacteristicImpl(dto.getId(), dto.getKey());
  }

  @Override
  public String getDescription() {
    return "Feed technical debt model";
  }
}
