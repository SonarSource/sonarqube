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
/* eslint-disable import/no-extraneous-dependencies */

import { screen } from '@testing-library/react';
import { render } from '../../helpers/testUtils';
import { MainMenuItem } from '../MainMenuItem';

it('should render default', () => {
  render(
    <MainMenuItem>
      <a>Hi</a>
    </MainMenuItem>,
  );

  expect(screen.getByText('Hi')).toHaveStyle({
    color: 'rgb(62, 67, 87)',
    'border-bottom': '4px solid transparent',
  });
});

it('should render active link', () => {
  render(
    <MainMenuItem>
      <a className="active">Hi</a>
    </MainMenuItem>,
  );

  expect(screen.getByText('Hi')).toHaveStyle({
    color: 'rgb(62, 67, 87)',
    'border-bottom': '4px solid rgba(123,135,217,1)',
  });
});

it('should render hovered link', () => {
  render(
    <MainMenuItem>
      <a className="hover">Hi</a>
    </MainMenuItem>,
  );

  expect(screen.getByText('Hi')).toHaveStyle({
    color: 'rgb(42, 47, 64)',
    'border-bottom': '4px solid rgba(123,135,217,1)',
  });
});
