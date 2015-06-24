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
///*
// * SonarQube, open source software quality management tool.
// * Copyright (C) 2008-2014 SonarSource
// * mailto:contact AT sonarsource DOT com
// *
// * SonarQube is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 3 of the License, or (at your option) any later version.
// *
// * SonarQube is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
// */
//
//package org.sonar.server.computation.step;
//
//import java.util.Collection;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.ClassRule;
//import org.junit.Test;
//import org.junit.experimental.categories.Category;
//import org.sonar.core.persistence.DbSession;
//import org.sonar.core.persistence.DbTester;
//import org.sonar.core.technicaldebt.db.CharacteristicDao;
//import org.sonar.server.computation.debt.Characteristic;
//import org.sonar.server.computation.debt.DebtModelHolderImpl;
//import org.sonar.server.computation.debt.MutableDebtModelHolder;
//import org.sonar.server.db.DbClient;
//import org.sonar.test.DbTests;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Category(DbTests.class)
//public class FeedDebtModelStepTest extends BaseStepTest {
//
//  @ClassRule
//  public static final DbTester dbTester = new DbTester();
//
//  DbClient dbClient;
//
//  DbSession dbSession;
//
//  MutableDebtModelHolder debtModelHolder = new DebtModelHolderImpl();
//
//  FeedDebtModelStep sut;
//
//  @Before
//  public void setUp() throws Exception {
//    dbTester.truncateTables();
//    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new CharacteristicDao(dbTester.myBatis()));
//    dbSession = dbClient.openSession(false);
//
//    sut = new FeedDebtModelStep(dbClient, debtModelHolder);
//  }
//
//  @After
//  public void tearDown() throws Exception {
//    dbSession.close();
//  }
//
//  @Override
//  protected ComputationStep step() {
//    return sut;
//  }
//
//  @Test
//  public void feed_characteristics() throws Exception {
//    dbTester.prepareDbUnit(getClass(), "shared.xml");
//
//    sut.execute();
//
//    Collection<Characteristic> rootChars = debtModelHolder.findRootCharacteristics();
//    assertThat(rootChars).extracting("id").containsOnly(1);
//    assertThat(rootChars).extracting("key").containsOnly("PORTABILITY");
//
//    Collection<Characteristic> subChars = debtModelHolder.findSubCharacteristicsByRootKey("PORTABILITY");
//    assertThat(subChars).extracting("id").containsOnly(2, 3);
//    assertThat(subChars).extracting("key").containsOnly("COMPILER_RELATED_PORTABILITY", "HARDWARE_RELATED_PORTABILITY");
//  }
//}
