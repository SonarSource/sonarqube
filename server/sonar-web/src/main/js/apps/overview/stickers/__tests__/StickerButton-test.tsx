/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import StickerButton from '../StickerButton';
import { StickerType } from '../utils';
import { click } from '../../../../helpers/testUtils';

it('should display correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
  expect(getWrapper({ selected: true })).toMatchSnapshot();
});

it('should return the sticker type on click', () => {
  const onClick = jest.fn();
  const wrapper = getWrapper({ onClick });
  click(wrapper.find('a'));
  expect(onClick).toHaveBeenCalledWith(StickerType.marketing);
});

function getWrapper(props = {}) {
  return shallow(
    <StickerButton
      onClick={jest.fn()}
      selected={false}
      type={StickerType.marketing}
      url="http://foo.bar"
      {...props}
    />
  );
}
