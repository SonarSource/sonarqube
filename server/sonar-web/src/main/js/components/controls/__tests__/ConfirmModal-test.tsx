/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import ConfirmModal from '../ConfirmModal';
import { submit, waitAndUpdate } from '../../../helpers/testUtils';

it('should render correctly', () => {
  const wrapper = shallow(
    <ConfirmModal
      confirmButtonText="confirm"
      confirmData="data"
      header="title"
      onClose={jest.fn()}
      onConfirm={jest.fn()}>
      <p>My confirm message</p>
    </ConfirmModal>
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('SimpleModal').dive()).toMatchSnapshot();
});

it('should confirm and close after confirm', async () => {
  const onClose = jest.fn();
  const onConfirm = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <ConfirmModal
      confirmButtonText="confirm"
      confirmData="data"
      header="title"
      onClose={onClose}
      onConfirm={onConfirm}>
      <p>My confirm message</p>
    </ConfirmModal>
  );
  const modalContent = wrapper.find('SimpleModal').dive();
  submit(modalContent.find('form'));
  expect(onConfirm).toBeCalledWith('data');
  expect(modalContent.find('footer')).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(onClose).toHaveBeenCalled();
});
