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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import QualityProfilesServiceMock from '../../../../api/mocks/QualityProfilesServiceMock';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import { mockQualityProfileChangelogEvent } from '../../../../helpers/testMocks';
import { renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import routes from '../../routes';

jest.mock('../../../../api/quality-profiles');

const serviceMock = new QualityProfilesServiceMock();
const settingsMock = new SettingsServiceMock();
const ui = {
  row: byRole('row'),
  cell: byRole('cell'),
  link: byRole('link'),
  emptyPage: byText('no_results'),
  showMore: byRole('button', { name: 'show_more' }),
  startDate: byRole('textbox', { name: 'start_date' }),
  endDate: byRole('textbox', { name: 'end_date' }),
  reset: byRole('button', { name: 'reset_verb' }),

  checkRow: (
    index: number,
    date: string,
    user: string,
    action: string,
    rule: string | null,
    updates: RegExp[] = [],
  ) => {
    const row = ui.row.getAll()[index];
    if (!row) {
      throw new Error(`Cannot find row ${index}`);
    }
    const cells = ui.cell.getAll(row);
    expect(cells[0]).toHaveTextContent(date);
    expect(cells[1]).toHaveTextContent(user);
    expect(cells[2]).toHaveTextContent(action);
    if (rule !== null) {
      expect(cells[3]).toHaveTextContent(rule);
    }
    for (const update of updates) {
      expect(cells[4]).toHaveTextContent(update);
    }
  },
};

beforeEach(() => {
  jest.useFakeTimers({
    advanceTimers: true,
    now: new Date('2019-04-25T03:12:32+0100'),
  });
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();

  serviceMock.reset();
  settingsMock.reset();
});

it('should see the changelog', async () => {
  const user = userEvent.setup();

  renderChangeLog();

  const rows = await ui.row.findAll();
  expect(rows).toHaveLength(6);
  expect(ui.emptyPage.query()).not.toBeInTheDocument();
  ui.checkRow(1, 'May 23, 2019', 'System', 'quality_profiles.changelog.ACTIVATED', 'Rule 0');
  ui.checkRow(2, 'April 23, 2019', 'System', 'quality_profiles.changelog.DEACTIVATED', 'Rule 0', [
    /quality_profiles.deprecated_severity_set_to severity.MAJOR/,
  ]);
  ui.checkRow(3, '', '', '', 'Rule 1', [
    /quality_profiles.deprecated_severity_set_to severity.CRITICAL/,
    /quality_profiles.changelog.cca_and_category_changed.*COMPLETE.*INTENTIONAL.*LAWFUL.*RESPONSIBLE/,
    /quality_profiles.changelog.impact_added.severity_impact.*MEDIUM.*RELIABILITY/,
    /quality_profiles.changelog.impact_removed.severity_impact.HIGH.*MAINTAINABILITY/,
  ]);
  await user.click(ui.link.get(rows[1]));
  expect(screen.getByText('/coding_rules?rule_key=c%3Arule0')).toBeInTheDocument();
});

it('should filter the changelog', async () => {
  const user = userEvent.setup();
  renderChangeLog();

  expect(await ui.row.findAll()).toHaveLength(6);
  await user.click(ui.startDate.get());
  await user.click(screen.getByRole('gridcell', { name: '20' }));
  await user.click(document.body);
  expect(await ui.row.findAll()).toHaveLength(5);
  await user.click(ui.endDate.get());
  await user.click(screen.getByRole('gridcell', { name: '25' }));
  await user.click(document.body);
  expect(await ui.row.findAll()).toHaveLength(4);
  await user.click(ui.reset.get());
  expect(await ui.row.findAll()).toHaveLength(6);
});

it('should load more', async () => {
  const user = userEvent.setup();
  serviceMock.changelogEvents = new Array(100).fill(null).map((_, i) =>
    mockQualityProfileChangelogEvent({
      ruleKey: `c:rule${i}`,
      ruleName: `Rule ${i}`,
    }),
  );
  renderChangeLog();

  expect(await ui.row.findAll()).toHaveLength(51);
  expect(ui.showMore.get()).toBeInTheDocument();
  await user.click(ui.showMore.get());
  expect(await ui.row.findAll()).toHaveLength(101);
  await user.click(ui.reset.get());
  expect(await ui.row.findAll()).toHaveLength(51);
});

it('should see short changelog for php', async () => {
  renderChangeLog('php', 'Good old PHP quality profile');

  const rows = await ui.row.findAll();
  expect(rows).toHaveLength(2);
  ui.checkRow(1, 'May 23, 2019', 'System', 'quality_profiles.changelog.DEACTIVATED', 'PHP Rule', [
    /quality_profiles.deprecated_severity_set_to severity.CRITICAL/,
    /quality_profiles.changelog.cca_and_category_changed.*COMPLETE.*INTENTIONAL.*CLEAR.*RESPONSIBLE/,
  ]);
  expect(ui.showMore.query()).not.toBeInTheDocument();
});

it('should show empty list for java', async () => {
  renderChangeLog('java', 'java quality profile');

  expect(await ui.emptyPage.find()).toBeInTheDocument();
  expect(ui.row.query()).not.toBeInTheDocument();
  expect(ui.showMore.query()).not.toBeInTheDocument();
});

function renderChangeLog(language = 'c', name = 'c quality profile') {
  renderAppRoutes(`profiles/changelog?name=${name}&language=${language}`, routes, {});
}
