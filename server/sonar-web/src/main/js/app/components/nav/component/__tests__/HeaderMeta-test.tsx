/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import HomePageSelect from '../../../../../components/controls/HomePageSelect';
import { mockBranch, mockPullRequest } from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockTaskWarning } from '../../../../../helpers/mocks/tasks';
import { mockCurrentUser } from '../../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../../types/component';
import { getCurrentPage, HeaderMeta, HeaderMetaProps } from '../HeaderMeta';

jest.mock('react-intl', () => ({
  useIntl: jest.fn().mockImplementation(() => ({
    formatDate: jest.fn().mockImplementation(() => '2017-01-02T00:00:00.000Z'),
  })),
}));

it('should render correctly for a branch', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for a main project branch', () => {
  const wrapper = shallowRender({
    branchLike: mockBranch({ isMain: true }),
  });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for a portfolio', () => {
  const wrapper = shallowRender({
    component: mockComponent({ key: 'foo', qualifier: ComponentQualifier.Portfolio }),
  });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for a pull request', () => {
  const wrapper = shallowRender({
    branchLike: mockPullRequest({
      url: 'https://example.com/pull/1234',
    }),
  });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly when the user is not logged in', () => {
  const wrapper = shallowRender({ currentUser: { isLoggedIn: false, dismissedNotices: {} } });
  expect(wrapper.find(HomePageSelect).exists()).toBe(false);
});

describe('#getCurrentPage', () => {
  it('should return a portfolio page', () => {
    expect(
      getCurrentPage(
        mockComponent({ key: 'foo', qualifier: ComponentQualifier.Portfolio }),
        undefined
      )
    ).toEqual({
      type: 'PORTFOLIO',
      component: 'foo',
    });
  });

  it('should return an application page', () => {
    expect(
      getCurrentPage(
        mockComponent({ key: 'foo', qualifier: ComponentQualifier.Application }),
        mockBranch({ name: 'develop' })
      )
    ).toEqual({ type: 'APPLICATION', component: 'foo', branch: 'develop' });
  });

  it('should return a project page', () => {
    expect(getCurrentPage(mockComponent(), mockBranch({ name: 'feature/foo' }))).toEqual({
      type: 'PROJECT',
      component: 'my-project',
      branch: 'feature/foo',
    });
  });
});

function shallowRender(props: Partial<HeaderMetaProps> = {}) {
  return shallow(
    <HeaderMeta
      branchLike={mockBranch()}
      component={mockComponent({ analysisDate: '2017-01-02T00:00:00.000Z', version: '0.0.1' })}
      currentUser={mockCurrentUser({ isLoggedIn: true })}
      onWarningDismiss={jest.fn()}
      warnings={[mockTaskWarning({ message: 'ERROR_1' }), mockTaskWarning({ message: 'ERROR_2' })]}
      {...props}
    />
  );
}
