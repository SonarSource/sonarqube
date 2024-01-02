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
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { mockEvent } from '../../../../helpers/testUtils';
import DeleteProfileForm, { DeleteProfileFormProps } from '../DeleteProfileForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ profile: mockQualityProfile({ childrenCount: 2 }) })).toMatchSnapshot(
    'profile has children'
  );
});

it('should correctly submit the form', () => {
  const onDelete = jest.fn();
  const wrapper = shallowRender({ onDelete });

  const formOnSubmit = wrapper.find('form').props().onSubmit;
  if (formOnSubmit) {
    formOnSubmit(mockEvent());
  }
  expect(onDelete).toHaveBeenCalled();
});

function shallowRender(props: Partial<DeleteProfileFormProps> = {}) {
  return shallow<DeleteProfileFormProps>(
    <DeleteProfileForm
      loading={false}
      onClose={jest.fn()}
      onDelete={jest.fn()}
      profile={mockQualityProfile()}
      {...props}
    />
  );
}
