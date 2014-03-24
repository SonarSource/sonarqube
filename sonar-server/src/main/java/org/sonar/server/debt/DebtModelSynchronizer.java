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

import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerExtension;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.TechnicalDebtModelRepository;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import javax.annotation.Nullable;

import java.io.Reader;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

// TODO replace this by DebtModelRestore
public class DebtModelSynchronizer implements ServerExtension {

  private final MyBatis mybatis;
  private final CharacteristicDao dao;
  private final TechnicalDebtModelRepository languageModelFinder;
  private final DebtCharacteristicsXMLImporter importer;

  public DebtModelSynchronizer(MyBatis mybatis, CharacteristicDao dao, TechnicalDebtModelRepository modelRepository, DebtCharacteristicsXMLImporter importer) {
    this.mybatis = mybatis;
    this.dao = dao;
    this.languageModelFinder = modelRepository;
    this.importer = importer;
  }

  public List<CharacteristicDto> synchronize() {
    SqlSession session = mybatis.openSession();

    List<CharacteristicDto> characteristics = newArrayList();
    try {
      DebtModel defaultModel = loadModelFromXml(TechnicalDebtModelRepository.DEFAULT_MODEL);
      List<CharacteristicDto> existingCharacteristics = dao.selectEnabledCharacteristics();
      if (existingCharacteristics.isEmpty()) {
        return createDebtModel(defaultModel, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return characteristics;
  }

  private List<CharacteristicDto> createDebtModel(DebtModel defaultModel, SqlSession session) {
    List<CharacteristicDto> characteristics = newArrayList();
    for (DebtCharacteristic rootCharacteristic : defaultModel.rootCharacteristics()) {
      CharacteristicDto rootCharacteristicDto = toDto(rootCharacteristic, null);
      dao.insert(rootCharacteristicDto, session);
      characteristics.add(rootCharacteristicDto);
      for (DebtCharacteristic characteristic : defaultModel.subCharacteristics(rootCharacteristic.key())) {
        CharacteristicDto characteristicDto = toDto(characteristic, rootCharacteristicDto.getId());
        dao.insert(characteristicDto, session);
        characteristics.add(characteristicDto);
      }
    }
    session.commit();
    return characteristics;
  }

  private DebtModel loadModelFromXml(String pluginKey) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      return importer.importXML(xmlFileReader);
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private static CharacteristicDto toDto(DebtCharacteristic characteristic, @Nullable Integer parentId) {
    return new CharacteristicDto()
      .setKey(characteristic.key())
      .setName(characteristic.name())
      .setOrder(characteristic.order())
      .setParentId(parentId)
      .setEnabled(true)
      .setCreatedAt(characteristic.createdAt())
      .setUpdatedAt(characteristic.updatedAt());
  }

}
