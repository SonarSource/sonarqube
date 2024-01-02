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
import { addFavorite, removeFavorite } from '../../../api/favorites';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../types/component';
import Favorite from '../Favorite';

jest.mock('../../../api/favorites', () => ({
  addFavorite: jest.fn().mockResolvedValue(null),
  removeFavorite: jest.fn().mockResolvedValue(null),
}));

it('renders and behaves correctly', async () => {
  renderFavorite({ favorite: false });
  let button = screen.getByRole('button');
  expect(button).toBeInTheDocument();

  button.click();
  await new Promise(setImmediate);
  expect(addFavorite).toHaveBeenCalled();

  button = screen.getByRole('button');
  expect(button).toBeInTheDocument();
  expect(button).toHaveFocus();

  button.click();
  await new Promise(setImmediate);
  expect(removeFavorite).toHaveBeenCalled();

  button = screen.getByRole('button');
  expect(button).toBeInTheDocument();
  expect(button).toHaveFocus();
});

it('correctly calls handleFavorite if passed', async () => {
  const handleFavorite = jest.fn();
  renderFavorite({ handleFavorite });

  screen.getByRole('button').click();
  await new Promise(setImmediate);
  expect(handleFavorite).toHaveBeenCalledWith('foo', false);

  screen.getByRole('button').click();
  await new Promise(setImmediate);
  expect(handleFavorite).toHaveBeenCalledWith('foo', true);
});

function renderFavorite(props: Partial<Favorite['props']> = {}) {
  return renderComponent(
    <Favorite component="foo" favorite qualifier={ComponentQualifier.Project} {...props} />,
  );
}
