/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { mount, shallow } from 'enzyme';
import App from '../App';
import { BranchType, LongLivingBranch } from '../../../../app/types';
import { isSonarCloud } from '../../../../helpers/system';

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

const component = {
  key: 'foo',
  analysisDate: '2016-01-01',
  breadcrumbs: [],
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

beforeEach(() => {
  (isSonarCloud as jest.Mock<any>).mockClear();
  (isSonarCloud as jest.Mock<any>).mockReturnValue(false);
});

it('should render OverviewApp', () => {
  expect(
    getWrapper()
      .find('Connect(OverviewApp)')
      .exists()
  ).toBeTruthy();
});

it('should render EmptyOverview', () => {
  expect(
    getWrapper({ component: { key: 'foo' } })
      .find('EmptyOverview')
      .exists()
  ).toBeTruthy();
});

it('should render SonarCloudEmptyOverview', () => {
  (isSonarCloud as jest.Mock<any>).mockReturnValue(true);
  expect(
    getWrapper({ component: { key: 'foo' } })
      .find('Connect(SonarCloudEmptyOverview)')
      .exists()
  ).toBeTruthy();
});

it('redirects on Code page for files', () => {
  const branch: LongLivingBranch = { isMain: false, name: 'b', type: BranchType.LONG };
  const newComponent = {
    ...component,
    breadcrumbs: [
      { key: 'project', name: 'Project', qualifier: 'TRK' },
      { key: 'foo', name: 'Foo', qualifier: 'DIR' }
    ],
    qualifier: 'FIL'
  };
  const replace = jest.fn();
  mount(
    <App
      branchLike={branch}
      branchLikes={[branch]}
      component={newComponent}
      onComponentChange={jest.fn()}
    />,
    {
      context: { router: { replace } }
    }
  );
  expect(replace).toBeCalledWith({
    pathname: '/code',
    query: { branch: 'b', id: 'project', selected: 'foo' }
  });
});

function getWrapper(props = {}) {
  return shallow(
    <App branchLikes={[]} component={component} onComponentChange={jest.fn()} {...props} />
  );
}
