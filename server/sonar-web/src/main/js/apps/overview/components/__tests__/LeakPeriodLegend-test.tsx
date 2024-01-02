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
import * as React from 'react';
import { IntlShape } from 'react-intl';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Period } from '../../../../types/types';
import { LeakPeriodLegend } from '../LeakPeriodLegend';

jest.mock('date-fns', () => {
  const actual = jest.requireActual('date-fns');
  return {
    ...actual,
    differenceInDays: jest.fn().mockReturnValue(10),
    differenceInYears: jest.fn().mockReturnValue(-9),
  };
});

it('10 days', async () => {
  renderLeakPeriodLegend({ mode: 'days', parameter: '10' });

  expect(
    await screen.findByText('overview.new_code_period_x.overview.period.days.10'),
  ).toBeInTheDocument();
});

it('date', async () => {
  renderLeakPeriodLegend({ mode: 'date', parameter: '2013-01-01' });

  expect(
    await screen.findByText('overview.new_code_period_x.overview.period.date.formatted.2013-01-01'),
  ).toBeInTheDocument();
  expect(await screen.findByText('overview.started_x.9 years ago')).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_on_x\..*/)).toBeInTheDocument();
});

it('version', async () => {
  renderLeakPeriodLegend({ mode: 'version', parameter: '0.1' });

  expect(
    await screen.findByText('overview.new_code_period_x.overview.period.version.0.1'),
  ).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*/)).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_on_x\..*/)).toBeInTheDocument();
});

it('previous_version', async () => {
  renderLeakPeriodLegend({ mode: 'previous_version' });

  expect(
    await screen.findByText(
      'overview.new_code_period_x.overview.period.previous_version_only_date',
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*/)).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_on_x\..*/)).toBeInTheDocument();
});

it('previous_analysis', async () => {
  renderLeakPeriodLegend({ mode: 'previous_analysis' });

  expect(
    await screen.findByText('overview.new_code_period_x.overview.period.previous_analysis.'),
  ).toBeInTheDocument();
  expect(await screen.findByText(/overview\.previous_analysis_x\..*/)).toBeInTheDocument();
  expect(await screen.findByText(/overview\.previous_analysis_x\..*/)).toBeInTheDocument();
});

it('manual_baseline', async () => {
  const rtl = renderLeakPeriodLegend({ mode: 'manual_baseline' });

  expect(
    await screen.findByText(
      /overview\.new_code_period_x\.overview\.period\.manual_baseline\.formattedTime\..*/,
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*/)).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_on_x\..*/)).toBeInTheDocument();

  rtl.unmount();
  renderLeakPeriodLegend({ mode: 'manual_baseline', parameter: '1.1.2' });

  expect(
    await screen.findByText('overview.new_code_period_x.overview.period.manual_baseline.1.1.2'),
  ).toBeInTheDocument();
  expect(
    await screen.findByText('overview.new_code_period_x.overview.period.manual_baseline.1.1.2'),
  ).toBeInTheDocument();
});

it('should render a more precise date', async () => {
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(0);

  renderLeakPeriodLegend({ date: '2018-08-17T00:00:00+0200', mode: 'previous_version' });

  expect(
    await screen.findByText(
      'overview.new_code_period_x.overview.period.previous_version_only_date',
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_x\..*/)).toBeInTheDocument();
  expect(await screen.findByText(/overview\.started_on_x\..*/)).toBeInTheDocument();
});

function renderLeakPeriodLegend(period: Partial<Period> = {}) {
  return renderComponent(
    <LeakPeriodLegend
      intl={
        {
          formatDate: (date: string) => 'formatted.' + date,
          formatTime: (date: string) => 'formattedTime.' + date,
        } as IntlShape
      }
      period={{
        date: '2013-09-22T00:00:00+0200',
        index: 0,
        mode: 'version',
        ...period,
      }}
    />,
  );
}
