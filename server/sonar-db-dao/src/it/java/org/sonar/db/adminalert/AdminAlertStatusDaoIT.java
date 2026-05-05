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
package org.sonar.db.adminalert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAlertStatusDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final AdminAlertStatusDao underTest = db.getDbClient().adminAlertStatusDao();

  @Test
  void findCurrentActiveByAlertKey_returns_empty_when_no_entry_exists() {
    assertThat(underTest.findCurrentActiveByAlertKey(db.getSession(), "UNKNOWN")).isEmpty();
  }

  @Test
  void findCurrentActiveByAlertKey_returns_empty_after_deactivation() {
    underTest.insertActivation(db.getSession(), "MY_ALERT");
    db.commit();
    underTest.deactivateCurrent(db.getSession(), "MY_ALERT");
    db.commit();

    assertThat(underTest.findCurrentActiveByAlertKey(db.getSession(), "MY_ALERT")).isEmpty();
  }

  @Test
  void findCurrentActiveByAlertKey_returns_active_entry_after_insertion() {
    underTest.insertActivation(db.getSession(), "MY_ALERT");
    db.commit();

    var result = underTest.findCurrentActiveByAlertKey(db.getSession(), "MY_ALERT");
    assertThat(result).isPresent();
    assertThat(result.get().getAlertKey()).isEqualTo("MY_ALERT");
    assertThat(result.get().isActive()).isTrue();
    assertThat(result.get().getActivatedAt()).isNotNull();
    assertThat(result.get().getDeactivatedAt()).isNull();
  }

  @Test
  void insertActivation_creates_new_historical_entry_on_reactivation() {
    // First activation
    underTest.insertActivation(db.getSession(), "MY_ALERT");
    db.commit();
    underTest.deactivateCurrent(db.getSession(), "MY_ALERT");
    db.commit();

    // Second activation
    underTest.insertActivation(db.getSession(), "MY_ALERT");
    db.commit();

    assertThat(underTest.findAll(db.getSession())).hasSize(2);
    assertThat(underTest.findCurrentActiveByAlertKey(db.getSession(), "MY_ALERT")).isPresent();
  }

  @Test
  void deactivateCurrent_sets_deactivated_at_and_clears_active_flag() {
    underTest.insertActivation(db.getSession(), "MY_ALERT");
    db.commit();

    underTest.deactivateCurrent(db.getSession(), "MY_ALERT");
    db.commit();

    var all = underTest.findAll(db.getSession());
    assertThat(all).hasSize(1);
    AdminAlertStatusDto entry = all.get(0);
    assertThat(entry.isActive()).isFalse();
    assertThat(entry.getDeactivatedAt()).isNotNull();
    assertThat(entry.getDeactivatedAt()).isGreaterThanOrEqualTo(entry.getActivatedAt());
  }

  @Test
  void deactivateCurrent_is_a_noop_when_no_active_entry_exists() {
    underTest.deactivateCurrent(db.getSession(), "MY_ALERT");
    db.commit();

    assertThat(underTest.findAll(db.getSession())).isEmpty();
  }

  @Test
  void findAllActive_returns_only_active_entries_across_all_alert_keys() {
    underTest.insertActivation(db.getSession(), "ALERT_A");
    underTest.insertActivation(db.getSession(), "ALERT_B");
    db.commit();
    underTest.deactivateCurrent(db.getSession(), "ALERT_B");
    db.commit();

    assertThat(underTest.findAllActive(db.getSession()))
      .extracting(AdminAlertStatusDto::getAlertKey)
      .containsExactly("ALERT_A");
  }

  @Test
  void findAll_returns_complete_history_ordered_by_activation_time() {
    underTest.insertActivation(db.getSession(), "ALERT_A");
    db.commit();
    underTest.deactivateCurrent(db.getSession(), "ALERT_A");
    db.commit();
    underTest.insertActivation(db.getSession(), "ALERT_A");
    db.commit();
    underTest.insertActivation(db.getSession(), "ALERT_B");
    db.commit();

    var all = underTest.findAll(db.getSession());
    assertThat(all).hasSize(3);
    assertThat(all.stream().filter(e -> !e.isActive())).hasSize(1);
    assertThat(all.stream().filter(AdminAlertStatusDto::isActive)).hasSize(2);
  }

  @Test
  void multiple_different_alerts_are_tracked_independently() {
    underTest.insertActivation(db.getSession(), "ALERT_A");
    underTest.insertActivation(db.getSession(), "ALERT_B");
    db.commit();
    underTest.deactivateCurrent(db.getSession(), "ALERT_A");
    db.commit();

    assertThat(underTest.findCurrentActiveByAlertKey(db.getSession(), "ALERT_A")).isEmpty();
    assertThat(underTest.findCurrentActiveByAlertKey(db.getSession(), "ALERT_B")).isPresent();
  }
}
