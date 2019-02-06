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
import { ComponentNavBranch } from '../ComponentNavBranch';
import { click } from '../../../../../helpers/testUtils';
import { isSonarCloud } from '../../../../../helpers/system';

jest.mock('../../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

const mainBranch: T.MainBranch = { isMain: true, name: 'master' };
const fooBranch: T.LongLivingBranch = { isMain: false, name: 'foo', type: 'LONG' };

beforeEach(() => {
  (isSonarCloud as jest.Mock).mockImplementation(() => false);
});

it('renders main branch', () => {
  const component = {} as T.Component;
  expect(
    shallow(
      <ComponentNavBranch
        appState={{ branchesEnabled: true }}
        branchLikes={[mainBranch, fooBranch]}
        component={component}
        currentBranchLike={mainBranch}
      />
    )
  ).toMatchSnapshot();
});

it('renders short-living branch', () => {
  const branch: T.ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'foo',
    status: { qualityGateStatus: 'OK' },
    type: 'SHORT'
  };
  const component = {} as T.Component;
  expect(
    shallow(
      <ComponentNavBranch
        appState={{ branchesEnabled: true }}
        branchLikes={[branch, fooBranch]}
        component={component}
        currentBranchLike={branch}
      />
    )
  ).toMatchSnapshot();
});

it('renders pull request', () => {
  const pullRequest: T.PullRequest = {
    base: 'master',
    branch: 'feature',
    key: '1234',
    title: 'Feature PR',
    url: 'https://example.com/pull/1234'
  };
  const component = {} as T.Component;
  expect(
    shallow(
      <ComponentNavBranch
        appState={{ branchesEnabled: true }}
        branchLikes={[pullRequest, fooBranch]}
        component={component}
        currentBranchLike={pullRequest}
      />
    )
  ).toMatchSnapshot();
});

it('opens menu', () => {
  const component = {} as T.Component;
  const wrapper = shallow(
    <ComponentNavBranch
      appState={{ branchesEnabled: true }}
      branchLikes={[mainBranch, fooBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />
  );
  expect(wrapper.find('Toggler').prop('open')).toBe(false);
  click(wrapper.find('a'));
  expect(wrapper.find('Toggler').prop('open')).toBe(true);
});

it('renders single branch popup', () => {
  const component = {} as T.Component;
  const wrapper = shallow(
    <ComponentNavBranch
      appState={{ branchesEnabled: true }}
      branchLikes={[mainBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />
  );
  expect(wrapper.find('DocTooltip')).toMatchSnapshot();
});

it('renders no branch support popup', () => {
  const component = {} as T.Component;
  const wrapper = shallow(
    <ComponentNavBranch
      appState={{ branchesEnabled: false }}
      branchLikes={[mainBranch, fooBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />
  );
  expect(wrapper.find('DocTooltip')).toMatchSnapshot();
});

it('renders nothing on SonarCloud without branch support', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const component = {} as T.Component;
  const wrapper = shallow(
    <ComponentNavBranch
      appState={{ branchesEnabled: false }}
      branchLikes={[mainBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />
  );
  expect(wrapper.type()).toBeNull();
});
