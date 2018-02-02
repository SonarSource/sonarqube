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
import { FormikProps } from 'formik';
import ValidationModal from '../ValidationModal';

it('should render correctly', () => {
  const { wrapper, inner } = getWrapper();
  expect(wrapper).toMatchSnapshot();
  expect(inner).toMatchSnapshot();
});

interface Values {
  field: string;
}

function getWrapper(props = {}) {
  const wrapper = shallow(
    <ValidationModal
      confirmButtonText="confirm"
      header="title"
      initialValues={{ field: 'foo' }}
      isInitialValid={true}
      onClose={jest.fn()}
      validate={(values: Values) => ({ field: values.field.length < 2 && 'Too small' })}
      onSubmit={jest.fn(() => Promise.resolve())}
      {...props}>
      {(props: FormikProps<Values>) => (
        <form onSubmit={props.handleSubmit}>
          <input
            onChange={props.handleChange}
            onBlur={props.handleBlur}
            name="field"
            type="text"
            value={props.values.field}
          />
        </form>
      )}
    </ValidationModal>
  );
  return {
    wrapper,
    inner: wrapper
      .childAt(0)
      .dive()
      .dive()
      .dive()
  };
}
