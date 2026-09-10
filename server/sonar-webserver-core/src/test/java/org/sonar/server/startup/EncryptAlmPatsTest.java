/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncryptAlmPatsTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final AlmPatDao almPatDao = mock(AlmPatDao.class);

  private final EncryptAlmPats underTest = new EncryptAlmPats(dbClient);

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @BeforeEach
  void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almPatDao()).thenReturn(almPatDao);
  }

  @Test
  void start_whenTokensRemainInClearText_shouldWarnThatNoSecretKeyIsConfigured() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(0);
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenReturn(0);
    when(almPatDao.countNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(4);

    underTest.start();

    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("4 DevOps platform personal access token(s) are stored as clear text"));
  }

  @Test
  void start_whenNoTokenRemainsInClearText_shouldNotWarn() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(0);
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenReturn(0);
    when(almPatDao.countNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(0);

    underTest.start();

    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void start_whenTokensWereStoredAsClearText_shouldEncryptThemAndReportHowMany() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(2);

    underTest.start();

    verify(almPatDao).encryptNotEncryptedPersonalAccessTokens(dbSession);
    assertThat(logTester.logs(Level.INFO))
      .anyMatch(log -> log.contains("Encrypted 2 DevOps platform personal access token(s)"));
  }

  @Test
  void start_whenEveryTokenIsAlreadyEncrypted_shouldNotReportAnything() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(0);

    underTest.start();

    verify(almPatDao).encryptNotEncryptedPersonalAccessTokens(dbSession);
    assertThat(logTester.logs(Level.INFO)).isEmpty();
  }

  @Test
  void start_whenEncryptingFails_shouldWarnRatherThanPreventStartup() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession))
      .thenThrow(new IllegalStateException("No secret key in the file: /etc/sonarqube/sonar-secret.txt"));

    assertThatNoException().isThrownBy(underTest::start);

    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("Failed to encrypt the DevOps platform personal access tokens that are stored as clear text"));
  }

  @Test
  void start_whenReEncryptingFails_shouldStillEncryptTheClearTextTokens() {
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenThrow(new IllegalStateException("connection is closed"));
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(2);

    assertThatNoException().isThrownBy(underTest::start);

    assertThat(logTester.logs(Level.INFO))
      .anyMatch(log -> log.contains("Encrypted 2 DevOps platform personal access token(s)"));
    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("Failed to re-encrypt the DevOps platform personal access tokens"))
      .noneMatch(log -> log.contains("Failed to encrypt the DevOps platform personal access tokens"));
  }

  @Test
  void start_whenCountingClearTextTokensFails_shouldWarnRatherThanPreventStartup() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(2);
    when(almPatDao.countNotEncryptedPersonalAccessTokens(dbSession)).thenThrow(new IllegalStateException("connection is closed"));

    assertThatNoException().isThrownBy(underTest::start);

    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("Failed to count the DevOps platform personal access tokens"));
    assertThat(logTester.logs(Level.INFO))
      .anyMatch(log -> log.contains("Encrypted 2 DevOps platform personal access token(s)"));
  }

  @Test
  void start_whenAnEarlierPassFails_shouldStillWarnAboutTheTokensLeftInClearText() {
    // the warning is the only thing that reports the exposure, so no other pass may take it down with it
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenThrow(new IllegalStateException("connection is closed"));
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenThrow(new IllegalStateException("connection is closed"));
    when(almPatDao.countNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(4);

    assertThatNoException().isThrownBy(underTest::start);

    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("4 DevOps platform personal access token(s) are stored as clear text"));
  }

  @Test
  void start_whenAPassFails_shouldRollBackWhatItLeftInTheSession() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenThrow(new IllegalStateException("connection is closed"));

    underTest.start();

    // the rows the failed pass had already updated must not be committed by the pass after it
    verify(dbSession).rollback();
  }

  @Test
  void start_whenRollingBackAFailedPassFails_shouldStillRunThePassesLeft() {
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenThrow(new IllegalStateException("connection is closed"));
    doThrow(new IllegalStateException("connection is closed")).when(dbSession).rollback();
    when(almPatDao.countNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(4);

    assertThatNoException().isThrownBy(underTest::start);

    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("4 DevOps platform personal access token(s) are stored as clear text"));
  }

  @Test
  void start_whenSecretKeyIsBeingReplaced_shouldReEncryptTokensAndReportHowMany() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(0);
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenReturn(3);

    underTest.start();

    assertThat(logTester.logs(Level.INFO))
      .anyMatch(log -> log.contains("Re-encrypted 3 DevOps platform personal access token(s) with the current secret key"));
  }

  @Test
  void start_whenNoSecretKeyIsBeingReplaced_shouldNotReportAnything() {
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession)).thenReturn(0);
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenReturn(0);

    underTest.start();

    verify(almPatDao).reEncryptPersonalAccessTokens(dbSession);
    assertThat(logTester.logs(Level.INFO)).isEmpty();
  }

  @Test
  void start_shouldRewriteTokensWithTheCurrentKeyBeforeEncryptingTheClearTextOnes() {
    // the clear-text pass writes with the current key, so a token it rewrote needs no rotation: were the rotation
    // pass to run after it, it would rewrite that token a second time and report a rotation that did not happen
    InOrder inOrder = inOrder(almPatDao);

    underTest.start();

    inOrder.verify(almPatDao).reEncryptPersonalAccessTokens(dbSession);
    inOrder.verify(almPatDao).encryptNotEncryptedPersonalAccessTokens(dbSession);
    // and the count runs last, so that a token it reports as clear text is one neither pass above could encrypt
    inOrder.verify(almPatDao).countNotEncryptedPersonalAccessTokens(dbSession);
  }

  @Test
  void start_whenAPassFailsAfterAnotherRewroteTokens_shouldOnlyReportTheOneThatFailed() {
    when(almPatDao.reEncryptPersonalAccessTokens(dbSession)).thenReturn(3);
    when(almPatDao.encryptNotEncryptedPersonalAccessTokens(dbSession))
      .thenThrow(new IllegalStateException("connection is closed"));

    assertThatNoException().isThrownBy(underTest::start);

    // the tokens the other pass rewrote are already durable, so the warning must not contradict its own log line
    assertThat(logTester.logs(Level.INFO))
      .anyMatch(log -> log.contains("Re-encrypted 3 DevOps platform personal access token(s)"));
    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("Failed to encrypt the DevOps platform personal access tokens that are stored as clear text"))
      .noneMatch(log -> log.contains("Failed to re-encrypt"));
  }
}
