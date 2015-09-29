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

import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.debt.DebtModelHolderImpl;
import org.sonar.server.computation.debt.MutableDebtModelHolder;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class LoadDebtModelStepTest extends BaseStepTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession;

  MutableDebtModelHolder debtModelHolder = new DebtModelHolderImpl();

  LoadDebtModelStep underTest;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    dbSession = dbClient.openSession(false);

    underTest = new LoadDebtModelStep(dbClient, debtModelHolder);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void feed_characteristics() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.execute();

    Collection<Characteristic> rootChars = debtModelHolder.getRootCharacteristics();
    assertThat(rootChars).extracting("id").containsOnly(1);
    assertThat(rootChars).extracting("key").containsOnly("PORTABILITY");

    Characteristic subChar = debtModelHolder.getCharacteristicById(1);
    assertThat(subChar).isNotNull();
  }
}
