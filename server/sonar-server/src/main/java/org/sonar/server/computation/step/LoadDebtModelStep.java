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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.debt.CharacteristicImpl;
import org.sonar.server.computation.debt.MutableDebtModelHolder;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

/**
 * Populates the {@link org.sonar.server.computation.debt.DebtModelHolder}
 */
public class LoadDebtModelStep implements ComputationStep {

  private final DbClient dbClient;
  private final MutableDebtModelHolder mutableDebtModelHolder;

  public LoadDebtModelStep(DbClient dbClient, MutableDebtModelHolder mutableDebtModelHolder) {
    this.dbClient = dbClient;
    this.mutableDebtModelHolder = mutableDebtModelHolder;
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
    Map<Integer, CharacteristicDto> rootCharacteristicsById = from(characteristicDtos)
      .filter(IsRootPredicate.INSTANCE)
      .uniqueIndex(CharacteristicDtoToId.INSTANCE);

    for (Map.Entry<Integer, Collection<CharacteristicDto>> entry : from(characteristicDtos)
      .filter(not(IsRootPredicate.INSTANCE))
      .index(CharacteristicDtoToParentId.INSTANCE)
      .asMap().entrySet()) {
      mutableDebtModelHolder.addCharacteristics(
        toCharacteristic(rootCharacteristicsById.get(entry.getKey())),
        from(entry.getValue()).transform(CharacteristicDtoToCharacteristic.INSTANCE)
        );
    }
  }

  private static Characteristic toCharacteristic(CharacteristicDto dto) {
    return new CharacteristicImpl(dto.getId(), dto.getKey(), dto.getParentId());
  }

  private enum CharacteristicDtoToId implements Function<CharacteristicDto, Integer> {
    INSTANCE;

    @Nullable
    @Override
    public Integer apply(@Nonnull CharacteristicDto dto) {
      return dto.getId();
    }
  }

  private enum CharacteristicDtoToCharacteristic implements Function<CharacteristicDto, Characteristic> {
    INSTANCE;

    @Nullable
    @Override
    public Characteristic apply(@Nonnull CharacteristicDto characteristicDto) {
      return toCharacteristic(characteristicDto);
    }
  }

  private enum CharacteristicDtoToParentId implements Function<CharacteristicDto, Integer> {
    INSTANCE;

    @Nullable
    @Override
    public Integer apply(@Nonnull CharacteristicDto characteristicDto) {
      Integer parentId = characteristicDto.getParentId();
      return parentId == null ? characteristicDto.getId() : parentId;
    }
  }

  private enum IsRootPredicate implements Predicate<CharacteristicDto> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull CharacteristicDto characteristicDto) {
      return characteristicDto.getParentId() == null;
    }
  }

  @Override
  public String getDescription() {
    return "Load debt model";
  }
}
