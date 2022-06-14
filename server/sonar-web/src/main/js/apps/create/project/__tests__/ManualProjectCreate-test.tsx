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
import { createProject, doesComponentExists } from '../../../../api/components';
import ProjectKeyInput from '../../../../components/common/ProjectKeyInput';
import { SubmitButton } from '../../../../components/controls/buttons';
import ValidationInput from '../../../../components/controls/ValidationInput';
import { validateProjectKey } from '../../../../helpers/projects';
import { change, mockEvent, submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { ProjectKeyValidationResult } from '../../../../types/component';
import { PROJECT_NAME_MAX_LEN } from '../constants';
import ManualProjectCreate from '../ManualProjectCreate';

jest.mock('../../../../api/components', () => ({
  createProject: jest.fn().mockResolvedValue({ project: { key: 'bar', name: 'Bar' } }),
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists'))
}));

jest.mock('../../../../helpers/projects', () => {
  const { PROJECT_KEY_INVALID_CHARACTERS } = jest.requireActual('../../../../helpers/projects');
  return {
    validateProjectKey: jest.fn(() => ProjectKeyValidationResult.Valid),
    PROJECT_KEY_INVALID_CHARACTERS
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();

  const wrapper = shallowRender();
  wrapper.instance().handleProjectNameChange('My new awesome app');
  expect(wrapper).toMatchSnapshot('with form filled');

  expect(shallowRender({ branchesEnabled: true })).toMatchSnapshot('with branches enabled');
});

it('should correctly create a project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = shallowRender({ onProjectCreate });

  wrapper
    .find(ProjectKeyInput)
    .props()
    .onProjectKeyChange(mockEvent({ currentTarget: { value: 'bar' } }));
  change(wrapper.find('input#project-name'), 'Bar');
  expect(wrapper.find(SubmitButton).props().disabled).toBe(false);
  expect(validateProjectKey).toBeCalledWith('bar');
  expect(doesComponentExists).toBeCalledWith({ component: 'bar' });

  submit(wrapper.find('form'));
  expect(createProject).toBeCalledWith({
    project: 'bar',
    name: 'Bar'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith('bar');
});

it('should not display any status when the name is not defined', () => {
  const wrapper = shallowRender();
  const projectNameInput = wrapper.find(ValidationInput);
  expect(projectNameInput.props().isInvalid).toBe(false);
  expect(projectNameInput.props().isValid).toBe(false);
});

it('should have an error when the key is invalid', () => {
  (validateProjectKey as jest.Mock).mockReturnValueOnce(ProjectKeyValidationResult.TooLong);

  const wrapper = shallowRender();

  wrapper.instance().handleProjectKeyChange('');
  expect(wrapper.find(ProjectKeyInput).props().error).toBe(
    `onboarding.create_project.project_key.error.${ProjectKeyValidationResult.TooLong}`
  );
});

it('should have an error when the key already exists', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleProjectKeyChange('exists', true);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectKeyError).toBe('onboarding.create_project.project_key.taken');
});

it('should ignore promise return if value has been changed in the meantime', async () => {
  (validateProjectKey as jest.Mock)
    .mockReturnValueOnce(ProjectKeyValidationResult.Valid)
    .mockReturnValueOnce(ProjectKeyValidationResult.InvalidChar);
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleProjectKeyChange('exists', true);
  instance.handleProjectKeyChange('exists%', true);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().projectKeyTouched).toBe(true);
  expect(wrapper.state().projectKeyError).toBe(
    `onboarding.create_project.project_key.error.${ProjectKeyValidationResult.InvalidChar}`
  );
});

it('should autofill the key based on the name, and sanitize it', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleProjectNameChange('newName', true);
  expect(wrapper.state().projectKey).toBe('newName');

  wrapper.instance().handleProjectNameChange('my invalid +"*ç%&/()= name', true);
  expect(wrapper.state().projectKey).toBe('my-invalid-name');
});

it.each([
  ['empty', ''],
  ['too_long', new Array(PROJECT_NAME_MAX_LEN + 1).fill('a').join('')]
])('should have an error when the name is %s', (errorSuffix: string, projectName: string) => {
  const wrapper = shallowRender();

  wrapper.instance().handleProjectNameChange(projectName, true);
  expect(wrapper.find(ValidationInput).props().isInvalid).toBe(true);
  expect(wrapper.state().projectNameError).toBe(
    `onboarding.create_project.display_name.error.${errorSuffix}`
  );
});

function shallowRender(props: Partial<ManualProjectCreate['props']> = {}) {
  return shallow<ManualProjectCreate>(
    <ManualProjectCreate branchesEnabled={false} onProjectCreate={jest.fn()} {...props} />
  );
}
