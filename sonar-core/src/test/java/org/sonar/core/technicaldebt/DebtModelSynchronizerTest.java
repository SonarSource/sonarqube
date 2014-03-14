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

package org.sonar.core.technicaldebt;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.io.Reader;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelSynchronizerTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  TechnicalDebtModelRepository technicalDebtModelRepository;

  @Mock
  CharacteristicDao dao;

  @Mock
  DebtCharacteristicsXMLImporter xmlImporter;

  Integer currentId = 1;

  private DefaultTechnicalDebtModel defaultModel;

  private DebtModelSynchronizer manager;

  @Before
  public void initAndMerge() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    defaultModel = new DefaultTechnicalDebtModel();
    Reader defaultModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("technical-debt")).thenReturn(defaultModelReader);
    when(xmlImporter.importXML(eq(defaultModelReader), any(ValidationMessages.class))).thenReturn(defaultModel);

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        CharacteristicDto dto = (CharacteristicDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(dao).insert(any(CharacteristicDto.class), any(SqlSession.class));


    manager = new DebtModelSynchronizer(myBatis, dao, technicalDebtModelRepository, xmlImporter);
  }

  @Test
  public void create_default_model_on_first_execution_when_no_plugin() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(rootCharacteristic);
    defaultModel.addRootCharacteristic(rootCharacteristic);

    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(Collections.<String>emptyList());
    when(dao.selectEnabledCharacteristics()).thenReturn(Lists.<CharacteristicDto>newArrayList());

    manager.synchronize(ValidationMessages.create());

    verify(dao).selectEnabledCharacteristics();
    ArgumentCaptor<CharacteristicDto> characteristicCaptor = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).insert(characteristicCaptor.capture(), eq(session));

    List<CharacteristicDto> result = characteristicCaptor.getAllValues();
    assertThat(result.get(0).getKey()).isEqualTo("PORTABILITY");
    assertThat(result.get(1).getKey()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void not_create_default_model_if_already_exists() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(rootCharacteristic);
    defaultModel.addRootCharacteristic(rootCharacteristic);

    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(Collections.<String>emptyList());
    when(dao.selectEnabledCharacteristics()).thenReturn(Lists.newArrayList(new CharacteristicDto()));

    manager.synchronize(ValidationMessages.create());

    verify(dao, never()).insert(any(CharacteristicDto.class), eq(session));
  }

}
