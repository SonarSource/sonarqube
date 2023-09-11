/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import QualityProfilesServiceMock from '../../../../api/mocks/QualityProfilesServiceMock';
import { mockQualityProfileChangelogEvent } from '../../../../helpers/testMocks';
import { renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import routes from '../../routes';

jest.mock('../../../../api/quality-profiles');

const serviceMock = new QualityProfilesServiceMock();
const ui = {
  row: byRole('row'),
  link: byRole('link'),
  emptyPage: byText('no_results'),
  showMore: byRole('link', { name: 'show_more' }),
  startDate: byRole('textbox', { name: 'start_date' }),
  endDate: byRole('textbox', { name: 'end_date' }),
  reset: byRole('button', { name: 'reset_verb' }),
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
});

it('should see the changelog', async () => {
  const user = userEvent.setup();
  renderChangeLog();

  const rows = await ui.row.findAll();
  expect(rows).toHaveLength(6);
  expect(ui.emptyPage.query()).not.toBeInTheDocument();
  expect(rows[1]).toHaveTextContent('May 23, 2019');
  expect(rows[1]).not.toHaveTextContent('quality_profiles.severity');
  expect(rows[2]).toHaveTextContent('April 23, 2019');
  expect(rows[2]).toHaveTextContent(
    'Systemquality_profiles.changelog.DEACTIVATEDRule 0quality_profiles.severity_set_to severity.MAJOR',
  );
  expect(rows[3]).not.toHaveTextContent('April 23, 2019');
  expect(rows[3]).not.toHaveTextContent('Systemquality_profiles.changelog.DEACTIVATED');
  expect(rows[3]).toHaveTextContent('Rule 1quality_profiles.severity_set_to severity.MAJOR');
  expect(rows[4]).toHaveTextContent('John Doe');
  expect(rows[4]).not.toHaveTextContent('System');
  expect(rows[5]).toHaveTextContent('March 23, 2019');
  expect(rows[5]).toHaveTextContent('John Doequality_profiles.changelog.ACTIVATEDRule 2');
  expect(rows[5]).toHaveTextContent(
    'quality_profiles.severity_set_to severity.CRITICALquality_profiles.parameter_set_to.credentialWords.foo,bar',
  );
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
  expect(rows[1]).toHaveTextContent('May 23, 2019');
  expect(rows[1]).toHaveTextContent(
    'Systemquality_profiles.changelog.DEACTIVATEDPHP Rulequality_profiles.severity_set_to severity.MAJOR',
  );
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
