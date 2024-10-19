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
import * as React from 'react';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import CoverageFilter from '../CoverageFilter';

it('renders options', () => {
  renderCoverageFilter({ value: 2 });

  expect(screen.getByRole('checkbox', { name: '≥ 80% 1' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: '70% - 80% 0' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: '50% - 70% 6' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: '30% - 50% 2' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: '< 30% 0' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: 'no_data 4' })).toBeInTheDocument();
});

it('updates the filter query', async () => {
  const user = userEvent.setup();

  const onQueryChange = jest.fn();

  renderCoverageFilter({ onQueryChange });

  await user.click(screen.getByRole('checkbox', { name: '50% - 70% 6' }));

  expect(onQueryChange).toHaveBeenCalledWith({ coverage: '3' });
});

function renderCoverageFilter(props: Partial<ComponentPropsType<typeof CoverageFilter>> = {}) {
  renderComponent(
    <CoverageFilter
      maxFacetValue={9}
      onQueryChange={jest.fn()}
      facet={{
        '80.0-*': 1,
        '70.0-80.0': 0,
        '50.0-70.0': 6,
        '30.0-50.0': 2,
        '*-30.0': 0,
        NO_DATA: 4,
      }}
      {...props}
    />,
  );
}
