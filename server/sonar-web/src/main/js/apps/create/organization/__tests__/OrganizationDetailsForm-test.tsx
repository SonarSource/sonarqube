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
import OrganizationDetailsForm from '../OrganizationDetailsForm';
import { click, submit } from '../../../../helpers/testUtils';

it('should render form', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.js-additional-info').prop('hidden')).toBe(true);

  click(wrapper.find('ResetButtonLink'));
  wrapper.update();
  expect(wrapper.find('.js-additional-info').prop('hidden')).toBe(false);
});

it('should validate before submit', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance() as OrganizationDetailsForm;

  expect(
    instance.canSubmit({
      additional: false,
      avatar: '',
      description: '',
      name: '',
      key: 'foo',
      submitting: false,
      url: ''
    })
  ).toBe(true);

  expect(
    instance.canSubmit({
      additional: false,
      avatar: '',
      description: '',
      name: '',
      key: undefined,
      submitting: false,
      url: ''
    })
  ).toBe(false);

  expect(
    instance.canSubmit({
      additional: false,
      avatar: undefined,
      description: '',
      name: '',
      key: 'foo',
      submitting: false,
      url: ''
    })
  ).toBe(false);

  instance.canSubmit = jest.fn() as any;
  submit(wrapper.find('form'));
  expect(instance.canSubmit).toHaveBeenCalled();
});

function shallowRender(props: Partial<OrganizationDetailsForm['props']> = {}) {
  return shallow(
    <OrganizationDetailsForm onContinue={jest.fn()} submitText="continue" {...props} />
  );
}
