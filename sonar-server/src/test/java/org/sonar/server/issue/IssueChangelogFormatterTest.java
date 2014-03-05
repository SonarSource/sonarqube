/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.utils.internal.WorkDuration;
import org.sonar.api.utils.internal.WorkDurationFactory;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.server.technicaldebt.DebtFormatter;

import java.util.List;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangelogFormatterTest {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  private static final int HOURS_IN_DAY = 8;

  @Mock
  private DefaultI18n i18n;

  @Mock
  private DebtFormatter debtFormatter;

  private IssueChangelogFormatter formatter;

  @Before
  public void before() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, HOURS_IN_DAY);
    formatter = new IssueChangelogFormatter(i18n, debtFormatter, new WorkDurationFactory(settings));
  }

  @Test
  public void format_field_diffs_with_new_and_old_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "BLOCKER", "INFO");

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.severity", null)).thenReturn("Severity");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.changed_to", null, "Severity", "INFO")).thenReturn("Severity changed to INFO");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.was", null, "BLOCKER")).thenReturn("was BLOCKER");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Severity changed to INFO (was BLOCKER)");
  }

  @Test
  public void format_field_diffs_with_only_new_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", null, "INFO");

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.severity", null)).thenReturn("Severity");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.changed_to", null, "Severity", "INFO")).thenReturn("Severity changed to INFO");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Severity changed to INFO");
  }

  @Test
  public void format_field_diffs_with_only_old_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "BLOCKER", null);

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.severity", null)).thenReturn("Severity");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.removed", null, "Severity")).thenReturn("Severity removed");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.was", null, "BLOCKER")).thenReturn("was BLOCKER");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Severity removed (was BLOCKER)");
  }

  @Test
  public void format_field_diffs_without_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", null, null);

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.severity", null)).thenReturn("Severity");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.removed", null, "Severity")).thenReturn("Severity removed");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Severity removed");
  }

  @Test
  public void format_field_diffs_with_empty_old_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "", null);

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.severity", null)).thenReturn("Severity");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.removed", null, "Severity")).thenReturn("Severity removed");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Severity removed");
  }

  @Test
  public void format_technical_debt_with_old_and_new_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("technicalDebt", "500", "10000");

    when(debtFormatter.format(DEFAULT_LOCALE, WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.HOURS, HOURS_IN_DAY))).thenReturn("5 hours");
    when(debtFormatter.format(DEFAULT_LOCALE, WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, HOURS_IN_DAY))).thenReturn("1 days");

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.technicalDebt", null)).thenReturn("Technical Debt");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.changed_to", null, "Technical Debt", "1 days")).thenReturn("Technical Debt changed to 1 days");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.was", null, "5 hours")).thenReturn("was 5 hours");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Technical Debt changed to 1 days (was 5 hours)");
  }

  @Test
  public void format_technical_debt_with_new_value_only() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("technicalDebt", null, "10000");

    when(debtFormatter.format(DEFAULT_LOCALE, WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, 8))).thenReturn("1 days");

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.technicalDebt", null)).thenReturn("Technical Debt");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.changed_to", null, "Technical Debt", "1 days")).thenReturn("Technical Debt changed to 1 days");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Technical Debt changed to 1 days");
  }

  @Test
  public void format_technical_debt_without_value() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("technicalDebt", null, null);

    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.field.technicalDebt", null)).thenReturn("Technical Debt");
    when(i18n.message(DEFAULT_LOCALE, "issue.changelog.removed", null, "Technical Debt")).thenReturn("Technical Debt removed");

    List<String> result = formatter.format(DEFAULT_LOCALE, diffs);
    assertThat(result).hasSize(1);
    String message = result.get(0);
    assertThat(message).isEqualTo("Technical Debt removed");
  }

}
