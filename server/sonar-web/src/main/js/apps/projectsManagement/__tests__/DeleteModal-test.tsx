/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
jest.mock('../../../api/components', () => ({
  deleteComponents: jest.fn(() => Promise.resolve())
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import DeleteModal, { Props } from '../DeleteModal';
import { click } from '../../../helpers/testUtils';

const deleteComponents = require('../../../api/components').deleteComponents as jest.Mock<any>;

it('deletes projects', async () => {
  const onConfirm = jest.fn();
  const wrapper = shallowRender({ onConfirm });
  (wrapper.instance() as DeleteModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('button'));
  expect(wrapper).toMatchSnapshot();
  expect(deleteComponents).toBeCalledWith(['foo', 'bar'], 'org');

  await new Promise(setImmediate);
  expect(onConfirm).toBeCalled();
});

it('closes', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  click(wrapper.find('.js-modal-close'));
  expect(onClose).toBeCalled();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <DeleteModal
      onClose={jest.fn()}
      onConfirm={jest.fn()}
      organization="org"
      qualifier="TRK"
      selection={['foo', 'bar']}
      {...props}
    />
  );
}
