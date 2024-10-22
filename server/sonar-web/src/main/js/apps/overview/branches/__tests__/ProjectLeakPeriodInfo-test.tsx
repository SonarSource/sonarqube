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
import { differenceInDays } from 'date-fns';
import { IntlShape } from 'react-intl';
import { mockPeriod } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import { Period } from '../../../../types/types';
import { ProjectLeakPeriodInfo } from '../ProjectLeakPeriodInfo';

jest.mock('date-fns', () => {
  const actual = jest.requireActual('date-fns');
  return { ...actual, differenceInDays: jest.fn().mockReturnValue(10) };
});

it('should render correctly for 10 days', async () => {
  renderProjectLeakPeriodInfo({ mode: 'days', parameter: '10' });
  expect(await screen.findByText('overview.period.days.10')).toBeInTheDocument();
});

it('should render correctly for a specific date', async () => {
  renderProjectLeakPeriodInfo({ mode: 'date', parameter: '2013-01-01' });
  expect(await screen.findByText('overview.period.date.formatted.2013-01-01')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*ago/)).toBeInTheDocument();
});

it('should render correctly for a specific version', async () => {
  renderProjectLeakPeriodInfo({ mode: 'version', parameter: '0.1' });
  expect(await screen.findByText('overview.period.version.0.1')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*ago/)).toBeInTheDocument();
});

it('should render correctly for "previous_version"', async () => {
  renderProjectLeakPeriodInfo({ mode: 'previous_version' });
  expect(await screen.findByText('overview.period.previous_version_only_date')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*ago/)).toBeInTheDocument();
});

it('should render correctly for "previous_analysis"', async () => {
  renderProjectLeakPeriodInfo({ mode: 'previous_analysis' });
  expect(await screen.findByText('overview.period.previous_analysis.')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.previous_analysis_x\..*ago/)).toBeInTheDocument();
});

it('should render correctly for "REFERENCE_BRANCH"', async () => {
  renderProjectLeakPeriodInfo({
    mode: NewCodeDefinitionType.ReferenceBranch,
    parameter: 'master',
  });
  expect(await screen.findByText('overview.period.reference_branch.master')).toBeInTheDocument();
});

it('should render correctly for "manual_baseline"', async () => {
  const rtl = renderProjectLeakPeriodInfo({ mode: 'manual_baseline' });

  expect(
    await screen.findByText(/overview\.period\.manual_baseline\.formattedTime\..*/),
  ).toBeInTheDocument();
  rtl.unmount();
  renderProjectLeakPeriodInfo({ mode: 'manual_baseline', parameter: '1.1.2' });
  expect(await screen.findByText('overview.period.manual_baseline.1.1.2')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*ago/)).toBeInTheDocument();
});

it('should render a more precise date', async () => {
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(0);
  renderProjectLeakPeriodInfo({
    date: '2018-08-17T00:00:00+0200',
    mode: 'previous_version',
  });
  expect(await screen.findByText('overview.period.previous_version_only_date')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*ago/)).toBeInTheDocument();
});

function renderProjectLeakPeriodInfo(period: Partial<Period> = {}) {
  return renderComponent(
    <ProjectLeakPeriodInfo
      intl={
        {
          formatDate: (date: string) => 'formatted.' + date,
          formatTime: (date: string) => 'formattedTime.' + date,
        } as IntlShape
      }
      leakPeriod={mockPeriod({ ...period })}
    />,
  );
}
