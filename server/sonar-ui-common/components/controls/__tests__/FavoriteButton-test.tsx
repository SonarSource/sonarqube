/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click } from '../../../helpers/testUtils';
import FavoriteButton, { Props } from '../FavoriteButton';

it('should render favorite', () => {
  const favorite = renderFavoriteBase({ favorite: true });
  expect(favorite).toMatchSnapshot();
});

it('should render not favorite', () => {
  const favorite = renderFavoriteBase({ favorite: false });
  expect(favorite).toMatchSnapshot();
});

it('should update properly', () => {
  const favorite = renderFavoriteBase({ favorite: false });
  expect(favorite).toMatchSnapshot();

  favorite.setProps({ favorite: true });
  expect(favorite).toMatchSnapshot();
});

it('should toggle favorite', () => {
  const toggleFavorite = jest.fn();
  const favorite = renderFavoriteBase({ toggleFavorite });
  click(favorite.find('ButtonLink'));
  expect(toggleFavorite).toBeCalled();
});

function renderFavoriteBase(props: Partial<Props> = {}) {
  return shallow(
    <FavoriteButton favorite={true} qualifier="TRK" toggleFavorite={jest.fn()} {...props} />
  );
}
