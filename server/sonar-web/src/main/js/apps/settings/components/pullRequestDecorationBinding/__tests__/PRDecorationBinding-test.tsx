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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  deleteProjectAlmBinding,
  getAlmSettings,
  getProjectAlmBinding,
  setProjectAlmBinding
} from '../../../../../api/almSettings';
import { mockComponent } from '../../../../../helpers/testMocks';
import PRDecorationBinding from '../PRDecorationBinding';

jest.mock('../../../../../api/almSettings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([]),
  getProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
  setProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
  deleteProjectAlmBinding: jest.fn().mockResolvedValue(undefined)
}));

const PROJECT_KEY = 'project-key';

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should fill selects and fill formdata', async () => {
  const url = 'github.com';
  const instances = [{ key: 'instance1', url, alm: 'github' }];
  const formdata = {
    key: 'instance1',
    repository: 'account/repo'
  };
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(formdata);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().hasBinding).toBe(true);
  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().formData).toEqual(formdata);
});

it('should preselect url and key if only 1 item', async () => {
  const instances = [{ key: 'instance1', url: 'github.enterprise.com', alm: 'github' }];
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  (getProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status: 404 });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().formData).toEqual({
    key: instances[0].key,
    repository: ''
  });
});

const formData = {
  key: 'whatever',
  repository: 'something/else'
};

it('should handle reset', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ formData });

  wrapper.instance().handleReset();
  await waitAndUpdate(wrapper);

  expect(deleteProjectAlmBinding).toBeCalledWith(PROJECT_KEY);
  expect(wrapper.state().formData).toEqual({ key: '', repository: '' });
  expect(wrapper.state().hasBinding).toBe(false);
});

it('should handle submit', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ formData });

  wrapper.instance().handleSubmit();
  await waitAndUpdate(wrapper);

  expect(setProjectAlmBinding).toBeCalledWith({
    almSetting: formData.key,
    project: PROJECT_KEY,
    repository: formData.repository
  });
  expect(wrapper.state().hasBinding).toBe(true);
  expect(wrapper.state().success).toBe(true);
});

it('should handle failures gracefully', async () => {
  (getProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status: 500 });
  (setProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status: 500 });
  (deleteProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status: 500 });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ formData });

  wrapper.instance().handleSubmit();
  await waitAndUpdate(wrapper);
  wrapper.instance().handleReset();
});

it('should handle field changes', async () => {
  const url = 'git.enterprise.com';
  const repository = 'my/repo';
  const instances = [
    { key: 'instance1', url, alm: 'github' },
    { key: 'instance2', url, alm: 'github' },
    { key: 'instance3', url: 'otherurl', alm: 'github' }
  ];
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleFieldChange('key', 'instance2');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    repository: ''
  });

  wrapper.instance().handleFieldChange('repository', repository);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    repository
  });
});

it('should validate form', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.instance().validateForm()).toBe(false);

  wrapper.setState({ formData: { key: '', repository: 'c' } });
  expect(wrapper.instance().validateForm()).toBe(false);

  wrapper.setState({ formData: { key: 'a', repository: 'c' } });
  expect(wrapper.instance().validateForm()).toBe(true);
});

function shallowRender(props: Partial<PRDecorationBinding['props']> = {}) {
  return shallow<PRDecorationBinding>(
    <PRDecorationBinding component={mockComponent({ key: PROJECT_KEY })} {...props} />
  );
}
