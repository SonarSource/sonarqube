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
import { get } from 'sonar-ui-common/helpers/storage';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { generateToken, getTokens } from '../../../../api/user-tokens';
import { mockComponent, mockLoggedInUser } from '../../../../helpers/testMocks';
import { getUniqueTokenName } from '../../utils';
import AnalyzeTutorialSonarCloud from '../AnalyzeTutorialSonarCloud';

jest.mock('sonar-ui-common/helpers/storage', () => ({
  get: jest.fn(),
  remove: jest.fn(),
  save: jest.fn()
}));

jest.mock('../../../../api/alm-integration', () => ({
  getGithubLanguages: jest.fn().mockResolvedValue({
    JavaScript: 512636,
    TypeScript: 425475,
    HTML: 390075,
    CSS: 14099,
    Makefile: 536,
    Dockerfile: 319
  })
}));

jest.mock('../../../../api/user-tokens', () => ({
  generateToken: jest.fn().mockResolvedValue({
    name: 'baz',
    createdAt: '2019-01-21T08:06:00+0100',
    login: 'luke',
    token: 'token_value'
  }),
  getTokens: jest.fn().mockResolvedValue([
    {
      name: 'foo',
      createdAt: '2019-01-15T15:06:33+0100',
      lastConnectionDate: '2019-01-18T15:06:33+0100'
    },
    { name: 'bar', createdAt: '2019-01-18T15:06:33+0100' }
  ]),
  revokeToken: jest.fn().mockResolvedValue(Promise.resolve())
}));

jest.mock('../../utils', () => ({
  getUniqueTokenName: jest.fn().mockReturnValue('lightsaber-9000')
}));

const component = mockComponent();

beforeEach(() => {
  jest.clearAllMocks();
});

const compGitHub = {
  ...component,
  alm: { key: 'github', url: 'https://github.com/luke/lightsaber' }
};

it('shows a loading screen', () => {
  const wrapper = shallowRender({ component: compGitHub });
  expect(wrapper.find('DeferredSpinner').prop('loading') as boolean).toBe(true);
});

it('renders for GitHub', async () => {
  const wrapper = shallowRender({ component: compGitHub });
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();

  wrapper
    .find('button')
    .first()
    .simulate('click');
  expect(wrapper.state('mode')).toEqual({
    id: 'autoscan',
    name: 'SonarCloud Automatic Analysis'
  });
});

it('renders for BitBucket', () => {
  const comp = {
    ...component,
    alm: { key: 'bitbucket', url: 'https://bitbucket.com/luke/lightsaber' }
  };
  const wrapper = shallowRender({ component: comp });

  expect(wrapper).toMatchSnapshot();

  (wrapper.find('TokenStep').prop('onContinue') as Function)('abc123');

  expect(wrapper.state('token')).toBe('abc123');
  expect(wrapper.state('step')).toBe('ANALYSIS');
});

it('renders for Azure', () => {
  const comp = {
    ...component,
    alm: { key: 'microsoft', url: 'https://azuredevops.com/luke/lightsaber' }
  };
  expect(shallowRender({ component: comp })).toMatchSnapshot();
});

it('renders for a manual project', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders the finished state', async () => {
  (get as jest.Mock).mockReturnValue('true');

  const comp = {
    ...component,
    alm: { key: 'github', url: 'https://github.com/luke/lightsaber' }
  };
  const wrapper = shallowRender({ component: comp });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('AnalyzeTutorialDone').exists()).toBe(true);
});

it('should get tokens and unique name', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(getTokens).toHaveBeenCalled();
  expect(getUniqueTokenName).toHaveBeenCalled();
  expect(generateToken).toHaveBeenCalled();
  expect(wrapper.state('token')).toBe('token_value');
});

it('should set tutorial done', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.setTutorialDone(false);
  expect(wrapper.state('isTutorialDone')).toBeFalsy();

  instance.setTutorialDone(true);
  expect(wrapper.state('isTutorialDone')).toBeTruthy();
});

it('should have a spinner', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(instance.spinner()).toMatchSnapshot();
});

function shallowRender(props: Partial<AnalyzeTutorialSonarCloud['props']> = {}) {
  return shallow<AnalyzeTutorialSonarCloud>(
    <AnalyzeTutorialSonarCloud
      component={component}
      currentUser={mockLoggedInUser({ externalProvider: 'github' })}
      {...props}
    />
  );
}
