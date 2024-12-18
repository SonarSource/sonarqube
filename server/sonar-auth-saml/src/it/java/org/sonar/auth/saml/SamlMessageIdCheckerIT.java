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
package org.sonar.auth.saml;

import org.junit.Rule;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

public class SamlMessageIdCheckerIT {

  @Rule
  public DbTester db = DbTester.create();

  private DbSession dbSession = db.getSession();

  //TODO

/*  private Auth auth = mock(Auth.class);

  private SamlMessageIdChecker underTest = new SamlMessageIdChecker(db.getDbClient());

  @Test
  public void check_do_not_fail_when_message_id_is_new_and_insert_saml_message_in_db() {
    db.getDbClient().samlMessageIdDao().insert(dbSession, new SamlMessageIdDto().setMessageId("MESSAGE_1").setExpirationDate(1_000_000_000L));
    db.commit();
    when(auth.getLastMessageId()).thenReturn("MESSAGE_2");
    when(auth.getLastAssertionNotOnOrAfter()).thenReturn(ImmutableList.of(Instant.ofEpochMilli(10_000_000_000L)));

    assertThatCode(() -> underTest.check(auth)).doesNotThrowAnyException();

    SamlMessageIdDto result = db.getDbClient().samlMessageIdDao().selectByMessageId(dbSession, "MESSAGE_2").get();
    assertThat(result.getMessageId()).isEqualTo("MESSAGE_2");
    assertThat(result.getExpirationDate()).isEqualTo(10_000_000_000L);
  }

  @Test
  public void check_fails_when_message_id_already_exist() {
    db.getDbClient().samlMessageIdDao().insert(dbSession, new SamlMessageIdDto().setMessageId("MESSAGE_1").setExpirationDate(1_000_000_000L));
    db.commit();
    when(auth.getLastMessageId()).thenReturn("MESSAGE_1");
    when(auth.getLastAssertionNotOnOrAfter()).thenReturn(ImmutableList.of(Instant.ofEpochMilli(10_000_000_000L)));

    assertThatThrownBy(() -> underTest.check(auth))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("This message has already been processed");
  }

  @Test
  public void check_insert_message_id_using_oldest_NotOnOrAfter_value() {
    db.getDbClient().samlMessageIdDao().insert(dbSession, new SamlMessageIdDto().setMessageId("MESSAGE_1").setExpirationDate(1_000_000_000L));
    db.commit();
    when(auth.getLastMessageId()).thenReturn("MESSAGE_2");
    when(auth.getLastAssertionNotOnOrAfter())
      .thenReturn(Arrays.asList(Instant.ofEpochMilli(10_000_000_000L), Instant.ofEpochMilli(30_000_000_000L), Instant.ofEpochMilli(20_000_000_000L)));

    assertThatCode(() -> underTest.check(auth)).doesNotThrowAnyException();

    SamlMessageIdDto result = db.getDbClient().samlMessageIdDao().selectByMessageId(dbSession, "MESSAGE_2").get();
    assertThat(result.getMessageId()).isEqualTo("MESSAGE_2");
    assertThat(result.getExpirationDate()).isEqualTo(10_000_000_000L);
  }*/
}
