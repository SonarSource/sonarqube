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
/* eslint-disable import/first, import/order */
jest.mock('../../../../api/ce', () => ({
  setWorkerCount: () => Promise.resolve()
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import WorkersForm from '../WorkersForm';
import { submit } from '../../../../helpers/testUtils';

it('changes select', () => {
  const wrapper = shallow(<WorkersForm onClose={jest.fn()} workerCount={1} />);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('Select').prop<Function>('onChange')({ value: 7 });
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('returns new worker count', async () => {
  const onClose = jest.fn();
  const wrapper = shallow(<WorkersForm onClose={onClose} workerCount={1} />);
  (wrapper.instance() as WorkersForm).mounted = true;
  wrapper.find('Select').prop<Function>('onChange')({ value: 7 });

  wrapper.update();
  submit(wrapper.find('form'));

  await new Promise(setImmediate);
  expect(onClose).toBeCalled();
});
