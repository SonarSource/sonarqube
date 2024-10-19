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
import userEvent from '@testing-library/user-event';
import { getDate, getMonth, getYear, subDays } from 'date-fns';
import { byPlaceholderText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import { now } from '../../../../helpers/dates';
import { getShortMonthName } from '../../../../helpers/l10n';
import { renderAppWithAdminContext } from '../../../../helpers/testReactTestingUtils';
import { AdminPageExtension } from '../../../../types/extension';
import { SettingsKey } from '../../../../types/settings';
import routes from '../../routes';
import { HousekeepingPolicy } from '../../utils';

const extensions = [
  { key: AdminPageExtension.GovernanceConsole, name: 'Portfolios' },
  { key: 'license/app', name: 'License Manager' },
  { key: 'license/support', name: 'Support' },
];

jest.mock('date-fns', () => {
  // Timezone will not play well so we fake the response from lib.
  const dateFns = jest.requireActual('date-fns');
  return {
    ...dateFns,
    endOfDay: jest.fn().mockImplementation((d) => d),
    startOfDay: jest.fn().mockImplementation((d) => d),
  };
});

jest.mock('../../../../helpers/dates', () => {
  return {
    ...jest.requireActual('../../../../helpers/dates'),
    now: jest.fn(() => new Date('2020-07-21T12:00:00Z')),
  };
});

const ui = {
  pageTitle: byRole('heading', { name: 'audit_logs.page' }),
  downloadButton: byRole('link', { name: 'download_verb' }),
  todayRadio: byRole('radio', { name: 'audit_logs.range_option.today' }),
  weekRadio: byRole('radio', { name: 'audit_logs.range_option.7days' }),
  monthRadio: byRole('radio', { name: 'audit_logs.range_option.30days' }),
  trimesterRadio: byRole('radio', { name: 'audit_logs.range_option.90days' }),
  customRadio: byRole('radio', { name: 'audit_logs.range_option.custom' }),
  downloadSentenceStart: byText('audit_logs.download_start.sentence.1'),
  startDateInput: byPlaceholderText('start_date'),
  endDateInput: byPlaceholderText('end_date'),
  dateInputMonthSelect: byTestId('month-select'),
  dateInputYearSelect: byTestId('year-select'),
};

let handler: SettingsServiceMock;

beforeAll(() => {
  handler = new SettingsServiceMock();
});

afterEach(() => handler.reset());

it('should handle download button click', async () => {
  const user = userEvent.setup();
  handler.set(SettingsKey.AuditHouseKeeping, HousekeepingPolicy.Yearly);
  renderAuditLogs();
  const downloadButton = await ui.downloadButton.find();
  expect(downloadButton).toBeInTheDocument();
  expect(downloadButton).toHaveAttribute(
    'href',
    '/api/audit_logs/download?from=2020-07-21T12%3A00%3A00.000Z&to=2020-07-21T12%3A00%3A00.000Z',
  );
  await user.click(ui.weekRadio.get());
  expect(downloadButton).toHaveAttribute(
    'href',
    '/api/audit_logs/download?from=2020-07-14T12%3A00%3A00.000Z&to=2020-07-21T12%3A00%3A00.000Z',
  );
  await user.click(ui.monthRadio.get());
  expect(downloadButton).toHaveAttribute(
    'href',
    '/api/audit_logs/download?from=2020-06-21T12%3A00%3A00.000Z&to=2020-07-21T12%3A00%3A00.000Z',
  );
  await user.click(ui.trimesterRadio.get());
  expect(downloadButton).toHaveAttribute(
    'href',
    '/api/audit_logs/download?from=2020-04-22T12%3A00%3A00.000Z&to=2020-07-21T12%3A00%3A00.000Z',
  );

  await user.click(downloadButton);

  expect(await ui.downloadButton.find()).toHaveAttribute('aria-disabled', 'true');
  expect(ui.downloadSentenceStart.get()).toBeInTheDocument();

  // Custom date
  const startDay = subDays(now(), 5);
  const endDate = subDays(now(), 1);
  await user.click(ui.customRadio.get());
  expect(ui.downloadButton.get()).toHaveAttribute('aria-disabled', 'true');
  await user.click(ui.startDateInput.get());
  const monthSelector = ui.dateInputMonthSelect.byRole('combobox').get();
  await user.click(monthSelector);
  await user.click(
    ui.dateInputMonthSelect
      .byText(getShortMonthName(getMonth(startDay)))
      .getAll()
      .slice(-1)[0],
  );

  const yearSelector = ui.dateInputYearSelect.byRole('combobox').get();
  await user.click(yearSelector);
  await user.click(
    ui.dateInputYearSelect.byText(getYear(startDay).toString()).getAll().slice(-1)[0],
  );

  await user.click(byText(getDate(startDay), { selector: 'button' }).get());

  await user.click(ui.endDateInput.get());

  await user.click(monthSelector);
  await user.click(
    ui.dateInputMonthSelect
      .byText(getShortMonthName(getMonth(endDate)))
      .getAll()
      .slice(-1)[0],
  );

  await user.click(yearSelector);
  await user.click(
    ui.dateInputYearSelect.byText(getYear(endDate).toString()).getAll().slice(-1)[0],
  );

  await user.click(byText(getDate(endDate), { selector: 'button' }).get());

  expect(await ui.downloadButton.find()).toHaveAttribute('aria-disabled', 'false');
  await user.click(downloadButton);
  expect(await ui.downloadButton.find()).toHaveAttribute('aria-disabled', 'true');
});

it('should not render if governance is not enable', () => {
  renderAuditLogs([]);
  expect(ui.pageTitle.query()).not.toBeInTheDocument();
});

it('should show right option when keeping log for month', async () => {
  handler.emptySettings();
  renderAuditLogs();
  expect(await ui.pageTitle.find()).toBeInTheDocument();
  expect(ui.todayRadio.get()).toBeInTheDocument();
  expect(ui.weekRadio.get()).toBeInTheDocument();
  expect(ui.monthRadio.get()).toBeInTheDocument();
  expect(ui.customRadio.get()).toBeInTheDocument();
  expect(ui.trimesterRadio.query()).not.toBeInTheDocument();
});

it('should show right option when keeping log for year', async () => {
  handler.set(SettingsKey.AuditHouseKeeping, HousekeepingPolicy.Yearly);
  renderAuditLogs();
  expect(await ui.pageTitle.find()).toBeInTheDocument();
  expect(ui.todayRadio.get()).toBeInTheDocument();
  expect(ui.weekRadio.get()).toBeInTheDocument();
  expect(ui.monthRadio.get()).toBeInTheDocument();
  expect(ui.trimesterRadio.get()).toBeInTheDocument();
  expect(ui.customRadio.get()).toBeInTheDocument();
});

function renderAuditLogs(adminPages = extensions) {
  renderAppWithAdminContext('admin/audit', routes, {}, { adminPages });
}
