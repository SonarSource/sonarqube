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
  createGithubConfiguration,
  deleteConfiguration,
  updateGithubConfiguration
} from '../../../../../api/almSettings';
import { mockGithubDefinition } from '../../../../../helpers/testMocks';
import GithubTab from '../GithubTab';

jest.mock('../../../../../api/almSettings', () => ({
  countBindedProjects: jest.fn().mockResolvedValue(2),
  createGithubConfiguration: jest.fn().mockResolvedValue({}),
  deleteConfiguration: jest.fn().mockResolvedValue({}),
  updateGithubConfiguration: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle cancel', async () => {
  const wrapper = shallowRender();

  wrapper.setState({
    definitionKeyForDeletion: '12321',
    definitionInEdition: mockGithubDefinition()
  });

  wrapper.instance().handleCancel();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().definitionKeyForDeletion).toBeUndefined();
  expect(wrapper.state().definitionInEdition).toBeUndefined();
});

it('should delete config', async () => {
  const onUpdateDefinitions = jest.fn();
  const wrapper = shallowRender({ onUpdateDefinitions });
  wrapper.setState({ definitionKeyForDeletion: '123' });

  await wrapper
    .instance()
    .deleteConfiguration('123')
    .then(() => {
      expect(deleteConfiguration).toBeCalledWith('123');
      expect(onUpdateDefinitions).toBeCalled();
      expect(wrapper.state().definitionKeyForDeletion).toBeUndefined();
    });
});

it('should create config', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = {
    key: 'new conf',
    url: 'ewrqewr',
    appId: '3742985',
    privateKey: 'rt7r78ew6t87ret'
  };
  const wrapper = shallowRender({ onUpdateDefinitions });
  wrapper.setState({ definitionInEdition: config });

  await wrapper
    .instance()
    .handleSubmit(config, '')
    .then(() => {
      expect(createGithubConfiguration).toBeCalledWith(config);
      expect(onUpdateDefinitions).toBeCalled();
      expect(wrapper.state().definitionInEdition).toBeUndefined();
    });
});

it('should update config', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = {
    key: 'new conf',
    url: 'ewrqewr',
    appId: '3742985',
    privateKey: 'rt7r78ew6t87ret'
  };
  const wrapper = shallowRender({ onUpdateDefinitions });
  wrapper.setState({ definitionInEdition: config });

  await wrapper
    .instance()
    .handleSubmit(config, 'originalKey')
    .then(() => {
      expect(updateGithubConfiguration).toBeCalledWith({
        newKey: 'new conf',
        ...config,
        key: 'originalKey'
      });
      expect(onUpdateDefinitions).toBeCalled();
      expect(wrapper.state().definitionInEdition).toBeUndefined();
    });
});

function shallowRender(props: Partial<GithubTab['props']> = {}) {
  return shallow<GithubTab>(
    <GithubTab definitions={[]} onUpdateDefinitions={jest.fn()} {...props} />
  );
}
