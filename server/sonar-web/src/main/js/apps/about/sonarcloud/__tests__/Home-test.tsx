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
import Home from '../Home';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { mockStore } from '../../../../helpers/testMocks';
import { requestHomepageData } from '../utils';

jest.mock('../utils', () => {
  const utils = require.requireActual('../utils');
  utils.requestHomepageData = jest.fn().mockResolvedValue({
    publicProjects: 236,
    publicLoc: 12345,
    pullRequests: 123456,
    rules: 1234,
    featuredProjects: [
      {
        key: 'sonarsource-jfrog.simple-js-php-project',
        avatarUrl: null,
        organizationKey: 'sonarsource-jfrog',
        organizationName: 'SonarSource & JFrog',
        name: 'Simple JS & PHP project',
        bugs: 0,
        codeSmells: 7,
        coverage: 9.7,
        duplications: 56.2,
        gateStatus: 'OK',
        languages: ['js', 'php'],
        maintainabilityRating: 1,
        ncloc: 123456,
        reliabilityRating: 1,
        securityRating: 1,
        vulnerabilities: 654321
      }
    ]
  });
  return utils;
});

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render', async () => {
  const wrapper = shallowRender('https://static.sonarcloud.io/homepage.json');
  expect(requestHomepageData).toBeCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('PageBackgroundHeader').dive()).toMatchSnapshot();
  expect(wrapper.find('PageTitle').dive()).toMatchSnapshot();
  expect(wrapper.find('EnhanceWorkflow').dive()).toMatchSnapshot();
  expect(wrapper.find('Functionality').dive()).toMatchSnapshot();
  expect(wrapper.find('Languages').dive()).toMatchSnapshot();
  expect(wrapper.find('Stats').dive()).toMatchSnapshot();
  expect(wrapper.find('Projects').dive()).toMatchSnapshot();
});

it('should not render real Stats and Projects', () => {
  const wrapper = shallowRender(undefined);
  expect(requestHomepageData).not.toBeCalled();
  expect(wrapper.find('Stats').dive()).toMatchSnapshot();
  expect(wrapper.find('Projects').dive()).toMatchSnapshot();
});

function shallowRender(homePageDataUrl: string | undefined) {
  return shallow(<Home />, {
    context: {
      store: mockStore({
        settingsApp: {
          values: {
            global: { 'sonar.homepage.url': { key: 'sonar.homepage.url', value: homePageDataUrl } }
          }
        }
      })
    }
  }).dive();
}
