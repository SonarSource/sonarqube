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
import * as React from 'react';
import { mockComponentMeasure } from '../../../../helpers/mocks/component';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { Period } from '../../../../types/types';
import LeakPeriodLegend, { LeakPeriodLegendProps } from '../LeakPeriodLegend';

jest.mock('date-fns', () => {
  const actual = jest.requireActual('date-fns');
  return { ...actual, differenceInDays: jest.fn().mockReturnValue(10) };
});

const PERIOD: Period = {
  date: '2017-05-16T13:50:02+0200',
  index: 1,
  mode: 'previous_version',
  parameter: '6,4',
};

const PERIOD_DAYS: Period = {
  date: '2017-05-16T13:50:02+0200',
  index: 1,
  mode: 'days',
  parameter: '18',
};

it('renders correctly for project', () => {
  renderLeakPeriodLegend();
  expect(screen.getByText('overview.period.previous_version.6,4')).toBeInTheDocument();
  expect(screen.getByText('component_measures.leak_legend.new_code')).toBeInTheDocument();
});

it('renders correctly for application', () => {
  renderLeakPeriodLegend({
    component: mockComponentMeasure(undefined, { qualifier: ComponentQualifier.Application }),
  });
  expect(screen.getByText('issues.new_code_period')).toBeInTheDocument();
});

it('renders correctly with big period', () => {
  renderLeakPeriodLegend({ period: PERIOD_DAYS });
  expect(screen.getByText('component_measures.leak_legend.new_code')).toBeInTheDocument();
  expect(screen.queryByText('overview.period.previous_version.6,4')).not.toBeInTheDocument();
});

function renderLeakPeriodLegend(overrides: Partial<LeakPeriodLegendProps> = {}) {
  return renderComponent(
    <LeakPeriodLegend component={mockComponentMeasure()} period={PERIOD} {...overrides} />,
  );
}
