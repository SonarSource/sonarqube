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
import QualityGateFacet from '../QualityGateFilter';

it('renders options', () => {
  renderQualityGateFilter();

  expect(screen.getByRole('checkbox', { name: 'metric.level.OK 6' })).toBeInTheDocument();
  expect(screen.getByRole('checkbox', { name: 'metric.level.ERROR 3' })).toBeInTheDocument();
});

it('updates the filter query', async () => {
  const user = userEvent.setup();

  const onQueryChange = jest.fn();

  renderQualityGateFilter({ onQueryChange });

  await user.click(screen.getByRole('checkbox', { name: 'metric.level.OK 6' }));

  expect(onQueryChange).toHaveBeenCalledWith({ gate: 'OK' });
});

it('handles multiselection', async () => {
  const user = userEvent.setup();

  const onQueryChange = jest.fn();

  renderQualityGateFilter({ onQueryChange, value: ['OK'] });

  await user.keyboard('{Control>}');
  await user.click(screen.getByRole('checkbox', { name: 'metric.level.ERROR 3' }));
  await user.keyboard('{/Control}');

  expect(onQueryChange).toHaveBeenCalledWith({ gate: 'OK,ERROR' });
});

function renderQualityGateFilter(props: Partial<ComponentPropsType<typeof QualityGateFacet>> = {}) {
  renderComponent(
    <QualityGateFacet
      maxFacetValue={9}
      onQueryChange={jest.fn()}
      facet={{ OK: 6, ERROR: 3 }}
      {...props}
    />,
  );
}
