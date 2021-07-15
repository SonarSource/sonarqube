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
import { waitAndUpdate } from '../../../helpers/testUtils';
import ValidationForm from '../ValidationForm';
import ValidationModal from '../ValidationModal';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find(ValidationForm).dive().dive()).toMatchSnapshot();
});

it('should handle submit', async () => {
  const data = { field: 'foo' };
  const onSubmit = jest.fn().mockResolvedValue({});
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose, onSubmit });

  wrapper.instance().handleSubmit(data);
  expect(onSubmit).toBeCalledWith(data);

  await waitAndUpdate(wrapper);
  expect(onClose).toBeCalled();
});

function shallowRender(props: Partial<ValidationModal<{ field: string }>['props']> = {}) {
  return shallow<ValidationModal<{ field: string }>>(
    <ValidationModal<{ field: string }>
      confirmButtonText="confirm"
      header="title"
      initialValues={{ field: 'foo' }}
      isDestructive={true}
      isInitialValid={true}
      onClose={jest.fn()}
      onSubmit={jest.fn()}
      validate={jest.fn()}
      {...props}>
      {(props) => (
        <input
          name="field"
          onBlur={props.handleBlur}
          onChange={props.handleChange}
          type="text"
          value={props.values.field}
        />
      )}
    </ValidationModal>
  );
}
