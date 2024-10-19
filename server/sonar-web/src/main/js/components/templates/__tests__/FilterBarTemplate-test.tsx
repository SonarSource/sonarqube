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
import React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { FCProps } from '../../../types/misc';
import FilterBarTemplate from '../FilterBarTemplate';

it('should render with filter header', () => {
  setupWithProps({ header: <span data-testid="filter-header">header</span>, headerHeight: 16 });

  expect(screen.getByTestId('filter-header')).toHaveTextContent('header');
});

function setupWithProps(props: Partial<FCProps<typeof FilterBarTemplate>> = {}) {
  return renderComponent(
    <FilterBarTemplate
      className="custom-class"
      content={<div data-testid="content" />}
      filterbar={<div data-testid="side" />}
      filterbarHeader={<div data-testid="side-header" />}
      size="large"
      {...props}
    />,
  );
}
