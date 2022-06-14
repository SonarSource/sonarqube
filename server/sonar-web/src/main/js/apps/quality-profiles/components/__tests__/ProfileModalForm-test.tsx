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
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { change, mockEvent } from '../../../../helpers/testUtils';
import ProfileModalForm, { ProfileModalFormProps } from '../ProfileModalForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');

  const wrapper = shallowRender();
  change(wrapper.find('#profile-name'), 'new name');
  expect(wrapper).toMatchSnapshot('can submit');
});

it('should correctly submit the form', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });

  // Won't submit unless a new name was given.
  let formOnSubmit = wrapper.find('form').props().onSubmit;
  if (formOnSubmit) {
    formOnSubmit(mockEvent());
  }
  expect(onSubmit).not.toBeCalled();

  // Input a new name.
  change(wrapper.find('#profile-name'), 'new name');

  // Now will submit the form.
  formOnSubmit = wrapper.find('form').props().onSubmit;
  if (formOnSubmit) {
    formOnSubmit(mockEvent());
  }
  expect(onSubmit).toBeCalledWith('new name');
});

function shallowRender(props: Partial<ProfileModalFormProps> = {}) {
  return shallow<ProfileModalFormProps>(
    <ProfileModalForm
      btnLabelKey="btn-label"
      headerKey="header-label"
      loading={false}
      onClose={jest.fn()}
      onSubmit={jest.fn()}
      profile={mockQualityProfile()}
      {...props}
    />
  );
}
