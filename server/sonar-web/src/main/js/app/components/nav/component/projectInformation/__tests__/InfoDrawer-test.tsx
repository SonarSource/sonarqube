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
import { shallow } from 'enzyme';
import * as React from 'react';
import { ClearButton } from '../../../../../../components/controls/buttons';
import InfoDrawer, { InfoDrawerProps } from '../InfoDrawer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ displayed: true })).toMatchSnapshot('displayed');
});

it('should call onClose when button is clicked', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose, displayed: true });
  wrapper.find(ClearButton).simulate('click');

  expect(onClose).toHaveBeenCalled();
});

function shallowRender(props: Partial<InfoDrawerProps> = {}) {
  return shallow(
    <InfoDrawer displayed={false} onClose={jest.fn()} top={120} {...props}>
      <span>content</span>
    </InfoDrawer>
  );
}
