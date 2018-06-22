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
package org.sonar.server.property;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.InternalPropertiesDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InternalPropertiesImplTest {
  private static final String EMPTY_STRING = "";
  public static final String SOME_VALUE = "a value";
  public static final String SOME_KEY = "some key";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private InternalPropertiesDao internalPropertiesDao = mock(InternalPropertiesDao.class);
  private InternalPropertiesImpl underTest = new InternalPropertiesImpl(dbClient);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.internalPropertiesDao()).thenReturn(internalPropertiesDao);
  }

  @Test
  public void reads_throws_IAE_if_key_is_null() {
    expectKeyNullOrEmptyIAE();

    underTest.read(null);
  }

  @Test
  public void reads_throws_IAE_if_key_is_empty() {
    expectKeyNullOrEmptyIAE();

    underTest.read(EMPTY_STRING);
  }

  @Test
  public void reads_returns_optional_from_DAO() {
    Optional<String> value = Optional.of("bablabla");

    when(internalPropertiesDao.selectByKey(dbSession, SOME_KEY)).thenReturn(value);

    assertThat(underTest.read(SOME_KEY)).isSameAs(value);
  }

  @Test
  public void write_throws_IAE_if_key_is_null() {
    expectKeyNullOrEmptyIAE();

    underTest.write(null, SOME_VALUE);
  }

  @Test
  public void writes_throws_IAE_if_key_is_empty() {
    expectKeyNullOrEmptyIAE();

    underTest.write(EMPTY_STRING, SOME_VALUE);
  }

  @Test
  public void write_calls_dao_saveAsEmpty_when_value_is_null() {
    underTest.write(SOME_KEY, null);

    verify(internalPropertiesDao).saveAsEmpty(dbSession, SOME_KEY);
    verify(dbSession).commit();
  }

  @Test
  public void write_calls_dao_saveAsEmpty_when_value_is_empty() {
    underTest.write(SOME_KEY, EMPTY_STRING);

    verify(internalPropertiesDao).saveAsEmpty(dbSession, SOME_KEY);
    verify(dbSession).commit();
  }

  @Test
  public void write_calls_dao_save_when_value_is_neither_null_nor_empty() {
    underTest.write(SOME_KEY, SOME_VALUE);

    verify(internalPropertiesDao).save(dbSession, SOME_KEY, SOME_VALUE);
    verify(dbSession).commit();
  }

  private void expectKeyNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("key can't be null nor empty");
  }
}
