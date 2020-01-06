/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
/* eslint-disable sonarjs/no-duplicate-string */
import { shallow } from 'enzyme';
import * as React from 'react';
import { change, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { createProject } from '../../../../api/components';
import ManualProjectCreate from '../ManualProjectCreate';

jest.mock('../../../../api/components', () => ({
  createProject: jest.fn().mockResolvedValue({ project: { key: 'bar', name: 'Bar' } }),
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists'))
}));

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(true)
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly create a public project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = shallowRender({ onProjectCreate });
  wrapper.find('withRouter(OrganizationInput)').prop<Function>('onChange')({ key: 'foo' });

  change(wrapper.find('input#project-key'), 'bar');
  change(wrapper.find('input#project-name'), 'Bar');
  expect(wrapper.find('SubmitButton').prop('disabled')).toBe(false);

  submit(wrapper.find('form'));
  expect(createProject).toBeCalledWith({
    project: 'bar',
    name: 'Bar',
    organization: 'foo',
    visibility: 'public'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith(['bar']);
});

it('should correctly create a private project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = shallowRender({ onProjectCreate });
  wrapper.find('withRouter(OrganizationInput)').prop<Function>('onChange')({ key: 'bar' });

  change(wrapper.find('input#project-key'), 'bar');
  change(wrapper.find('input#project-name'), 'Bar');

  submit(wrapper.find('form'));
  expect(createProject).toBeCalledWith({
    project: 'bar',
    name: 'Bar',
    organization: 'bar',
    visibility: 'private'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith(['bar']);
});

it('should not display any status when the key is not defined', () => {
  const wrapper = shallowRender();
  const projectKeyInput = wrapper.find('ValidationInput').first();
  expect(projectKeyInput.prop('isInvalid')).toBe(false);
  expect(projectKeyInput.prop('isValid')).toBe(false);
});

it('should not display any status when the name is not defined', () => {
  const wrapper = shallowRender();
  const projectKeyInput = wrapper.find('ValidationInput').last();
  expect(projectKeyInput.prop('isInvalid')).toBe(false);
  expect(projectKeyInput.prop('isValid')).toBe(false);
});

it('should have an error when the key is invalid', () => {
  const wrapper = shallowRender();
  change(wrapper.find('input#project-key'), 'KEy-with#speci@l_char');
  expect(
    wrapper
      .find('ValidationInput')
      .first()
      .prop('isInvalid')
  ).toBe(true);
});

it('should have an error when the key already exists', async () => {
  const wrapper = shallowRender();
  change(wrapper.find('input#project-key'), 'exists');

  await waitAndUpdate(wrapper);
  expect(
    wrapper
      .find('ValidationInput')
      .first()
      .prop('isInvalid')
  ).toBe(true);
});

it('should ignore promise return if value has been changed in the meantime', async () => {
  const wrapper = shallowRender();

  change(wrapper.find('input#project-key'), 'exists');
  change(wrapper.find('input#project-key'), 'exists%');

  await waitAndUpdate(wrapper);

  expect(wrapper.state('touched')).toBe(true);
  expect(wrapper.state('projectKeyError')).toBe('onboarding.create_project.project_key.error');
});

it('should autofill the name based on the key', () => {
  const wrapper = shallowRender();
  change(wrapper.find('input#project-key'), 'bar');
  expect(wrapper.find('input#project-name').prop('value')).toBe('bar');
});

it('should have an error when the name is empty', () => {
  const wrapper = shallowRender();
  change(wrapper.find('input#project-key'), 'bar');
  change(wrapper.find('input#project-name'), '');
  expect(
    wrapper
      .find('ValidationInput')
      .last()
      .prop('isInvalid')
  ).toBe(true);
  expect(wrapper.state('projectNameError')).toBe('onboarding.create_project.display_name.error');
});

function shallowRender(props: Partial<ManualProjectCreate['props']> = {}) {
  return shallow<ManualProjectCreate>(
    <ManualProjectCreate
      currentUser={{ groups: [], isLoggedIn: true, login: 'foo', name: 'Foo', scmAccounts: [] }}
      fetchMyOrganizations={jest.fn()}
      onProjectCreate={jest.fn()}
      userOrganizations={[
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar', subscription: 'PAID' }
      ]}
      {...props}
    />
  );
}
