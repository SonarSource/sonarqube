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
import VisibilitySelector from '../VisibilitySelector';
import { click } from '../../../helpers/testUtils';

it('changes visibility', () => {
  const onChange = jest.fn();
  const wrapper = shallow(
    <VisibilitySelector canTurnToPrivate={true} onChange={onChange} visibility="public" />
  );
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('#visibility-private'));
  expect(onChange).toBeCalledWith('private');

  wrapper.setProps({ visibility: 'private' });
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('#visibility-public'));
  expect(onChange).toBeCalledWith('public');
});

it('renders disabled', () => {
  expect(
    shallow(
      <VisibilitySelector canTurnToPrivate={false} onChange={jest.fn()} visibility="public" />
    )
  ).toMatchSnapshot();
});
