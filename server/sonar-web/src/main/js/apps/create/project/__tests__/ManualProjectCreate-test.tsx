/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import ValidationInput from 'sonar-ui-common/components/controls/ValidationInput';
import { change, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { createProject, doesComponentExists } from '../../../../api/components';
import ProjectKeyInput from '../../../../components/common/ProjectKeyInput';
import { validateProjectKey } from '../../../../helpers/projects';
import { mockEvent } from '../../../../helpers/testMocks';
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
  const { ProjectKeyValidationResult } = jest.requireActual('../../../../types/component');
  return { validateProjectKey: jest.fn(() => ProjectKeyValidationResult.Valid) };
});

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
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
  expect(onProjectCreate).toBeCalledWith(['bar']);
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
  const instance = wrapper.instance();
  instance.handleProjectKeyChange(mockEvent());
  expect(wrapper.find(ProjectKeyInput).props().error).toBe(
    `onboarding.create_project.project_key.error.${ProjectKeyValidationResult.TooLong}`
  );
});

it('should have an error when the key already exists', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleProjectKeyChange(mockEvent({ currentTarget: { value: 'exists' } }));
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectKeyError).toBe('onboarding.create_project.project_key.taken');
});

it('should ignore promise return if value has been changed in the meantime', async () => {
  (validateProjectKey as jest.Mock)
    .mockReturnValueOnce(ProjectKeyValidationResult.Valid)
    .mockReturnValueOnce(ProjectKeyValidationResult.InvalidChar);
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleProjectKeyChange(mockEvent({ currentTarget: { value: 'exists' } }));
  instance.handleProjectKeyChange(mockEvent({ currentTarget: { value: 'exists%' } }));

  await waitAndUpdate(wrapper);

  expect(wrapper.state().touched).toBe(true);
  expect(wrapper.state().projectKeyError).toBe(
    `onboarding.create_project.project_key.error.${ProjectKeyValidationResult.InvalidChar}`
  );
});

it('should autofill the name based on the key', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleProjectKeyChange(mockEvent({ currentTarget: { value: 'bar' } }));
  expect(wrapper.find('input#project-name').props().value).toBe('bar');
});

it('should have an error when the name is incorrect', () => {
  const wrapper = shallowRender();
  wrapper.setState({ touched: true });
  const instance = wrapper.instance();

  instance.handleProjectNameChange(mockEvent({ currentTarget: { value: '' } }));
  expect(wrapper.find(ValidationInput).props().isInvalid).toBe(true);
  expect(wrapper.state().projectNameError).toBe(
    'onboarding.create_project.display_name.error.empty'
  );

  instance.handleProjectNameChange(
    mockEvent({ currentTarget: { value: new Array(PROJECT_NAME_MAX_LEN + 1).fill('a').join('') } })
  );
  expect(wrapper.find(ValidationInput).props().isInvalid).toBe(true);
  expect(wrapper.state().projectNameError).toBe(
    'onboarding.create_project.display_name.error.too_long'
  );
});

function shallowRender(props: Partial<ManualProjectCreate['props']> = {}) {
  return shallow<ManualProjectCreate>(
    <ManualProjectCreate onProjectCreate={jest.fn()} {...props} />
  );
}
