/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { createQualityGate } from '../../../../api/quality-gates';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { mockRouter } from '../../../../helpers/testMocks';
import { change, waitAndUpdate } from '../../../../helpers/testUtils';
import { getQualityGateUrl } from '../../../../helpers/urls';
import { CreateQualityGateForm } from '../CreateQualityGateForm';

jest.mock('../../../../api/quality-gates', () => ({
  createQualityGate: jest.fn().mockResolvedValue({ id: '1', name: 'newValue' })
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly handle create', async () => {
  const onCreate = jest.fn().mockResolvedValue(undefined);
  const push = jest.fn();
  const wrapper = shallowRender({ onCreate, router: mockRouter({ push }) });

  wrapper
    .find(ConfirmModal)
    .props()
    .onConfirm();
  expect(createQualityGate).not.toHaveBeenCalled();

  change(wrapper.find('#quality-gate-form-name'), 'newValue');
  expect(wrapper.state().name).toBe('newValue');

  wrapper
    .find(ConfirmModal)
    .props()
    .onConfirm();
  expect(createQualityGate).toHaveBeenCalledWith({ name: 'newValue' });

  await waitAndUpdate(wrapper);
  expect(onCreate).toHaveBeenCalled();
  expect(push).toHaveBeenCalledWith(getQualityGateUrl('1'));
});

function shallowRender(props: Partial<CreateQualityGateForm['props']> = {}) {
  return shallow<CreateQualityGateForm>(
    <CreateQualityGateForm
      onClose={jest.fn()}
      onCreate={jest.fn()}
      router={mockRouter()}
      {...props}
    />
  );
}
