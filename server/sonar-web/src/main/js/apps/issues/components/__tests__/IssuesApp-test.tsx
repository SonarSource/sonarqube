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
import { searchIssues } from '../../../../api/issues';
import { getRuleDetails } from '../../../../api/rules';
import handleRequiredAuthentication from '../../../../helpers/handleRequiredAuthentication';
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../../../../helpers/pages';
import {
  mockCurrentUser,
  mockIssue,
  mockLocation,
  mockLoggedInUser,
  mockRawIssue,
  mockRouter,
} from '../../../../helpers/testMocks';
import { keydown, mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import { ReferencedComponent } from '../../../../types/issues';
import { Issue, Paging } from '../../../../types/types';
import {
  disableLocationsNavigator,
  enableLocationsNavigator,
  selectNextFlow,
  selectNextLocation,
  selectPreviousFlow,
  selectPreviousLocation,
} from '../../actions';
import BulkChangeModal from '../BulkChangeModal';
import { App } from '../IssuesApp';

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  addWhitePageClass: jest.fn(),
  removeSideBarClass: jest.fn(),
  removeWhitePageClass: jest.fn(),
}));

jest.mock('../../../../helpers/handleRequiredAuthentication', () => jest.fn());

jest.mock('../../../../api/issues', () => ({
  searchIssues: jest.fn().mockResolvedValue({ facets: [], issues: [] }),
}));

jest.mock('../../../../api/rules', () => ({
  getRuleDetails: jest.fn(),
}));

jest.mock('../../../../api/users', () => ({
  getCurrentUser: jest.fn().mockResolvedValue({
    dismissedNotices: {
      something: false,
    },
  }),
  dismissNotification: jest.fn(),
}));

const RAW_ISSUES = [
  mockRawIssue(false, { key: 'foo' }),
  mockRawIssue(false, { key: 'bar' }),
  mockRawIssue(true, { key: 'third' }),
  mockRawIssue(false, { key: 'fourth' }),
];
const ISSUES = [
  mockIssue(false, { key: 'foo' }),
  mockIssue(false, { key: 'bar' }),
  mockIssue(true, { key: 'third' }),
  mockIssue(false, { key: 'fourth' }),
];
const FACETS = [{ property: 'severities', values: [{ val: 'MINOR', count: 4 }] }];
const PAGING = { pageIndex: 1, pageSize: 100, total: 4 };

const referencedComponent: ReferencedComponent = { key: 'foo-key', name: 'bar', uuid: 'foo-uuid' };

beforeEach(() => {
  (searchIssues as jest.Mock).mockResolvedValue({
    components: [referencedComponent],
    effortTotal: 1,
    facets: FACETS,
    issues: RAW_ISSUES,
    languages: [],
    paging: PAGING,
    rules: [],
    users: [],
  });

  (getRuleDetails as jest.Mock).mockResolvedValue({ rule: { test: 'test' } });
});

afterEach(() => {
  (searchIssues as jest.Mock).mockReset();
});

it('should render a list of issue', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);
  expect(wrapper.state().referencedComponentsById).toEqual({ 'foo-uuid': referencedComponent });
  expect(wrapper.state().referencedComponentsByKey).toEqual({ 'foo-key': referencedComponent });

  expect(addSideBarClass).toHaveBeenCalled();
  expect(addWhitePageClass).toHaveBeenCalled();
});

it('should handle my issue change properly', () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });
  wrapper.instance().handleMyIssuesChange(true);

  expect(push).toHaveBeenCalledWith({
    pathname: '/issues',
    query: {
      id: 'foo',
      author: [],
      myIssues: 'true',
    },
  });
});

it('should load search result count correcly', async () => {
  const wrapper = shallowRender();
  const count = await wrapper.instance().loadSearchResultCount('severities', {});
  expect(count).toStrictEqual({ MINOR: 4 });
});

it('should not render for anonymous user', () => {
  shallowRender({
    currentUser: mockCurrentUser({ isLoggedIn: false }),
    location: mockLocation({ query: { myIssues: true.toString() } }),
  });
  expect(handleRequiredAuthentication).toHaveBeenCalled();
});

it('should handle reset properly', () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });
  wrapper.instance().handleReset();
  expect(push).toHaveBeenCalledWith({
    pathname: '/issues',
    query: {
      id: 'foo',
      myIssues: undefined,
      resolved: 'false',
    },
  });
});

it('should open standard facets for vulnerabilities and hotspots', () => {
  const wrapper = shallowRender({
    location: mockLocation({ pathname: '/issues', query: { types: 'VULNERABILITY' } }),
  });
  const instance = wrapper.instance();
  const fetchFacet = jest.spyOn(instance, 'fetchFacet');

  expect(wrapper.state('openFacets').standards).toEqual(true);
  expect(wrapper.state('openFacets').sonarsourceSecurity).toEqual(true);

  instance.handleFacetToggle('standards');
  expect(wrapper.state('openFacets').standards).toEqual(false);
  expect(fetchFacet).not.toHaveBeenCalled();

  instance.handleFacetToggle('standards');
  expect(wrapper.state('openFacets').standards).toEqual(true);
  expect(wrapper.state('openFacets').sonarsourceSecurity).toEqual(true);
  expect(fetchFacet).toHaveBeenLastCalledWith('sonarsourceSecurity');

  instance.handleFacetToggle('owaspTop10');
  expect(wrapper.state('openFacets').owaspTop10).toEqual(true);
  expect(fetchFacet).toHaveBeenLastCalledWith('owaspTop10');
});

it('should correctly bind key events for issue navigation', async () => {
  const push = jest.fn();
  const addEventListenerSpy = jest.spyOn(document, 'addEventListener');
  const wrapper = shallowRender({ router: mockRouter({ push }) });
  await waitAndUpdate(wrapper);

  expect(addEventListenerSpy).toHaveBeenCalledTimes(2);

  expect(wrapper.state('selected')).toBe(ISSUES[0].key);

  keydown({ key: KeyboardKeys.DownArrow });
  expect(wrapper.state('selected')).toBe(ISSUES[1].key);

  keydown({ key: KeyboardKeys.UpArrow });
  keydown({ key: KeyboardKeys.UpArrow });
  expect(wrapper.state('selected')).toBe(ISSUES[0].key);

  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  expect(wrapper.state('selected')).toBe(ISSUES[3].key);

  keydown({ key: KeyboardKeys.RightArrow, ctrlKey: true });
  expect(push).not.toHaveBeenCalled();
  keydown({ key: KeyboardKeys.RightArrow });
  expect(push).toHaveBeenCalledTimes(1);

  keydown({ key: KeyboardKeys.LeftArrow });
  expect(push).toHaveBeenCalledTimes(2);

  addEventListenerSpy.mockReset();
});

it('should correctly clean up on unmount', () => {
  const removeEventListenerSpy = jest.spyOn(document, 'removeEventListener');
  const wrapper = shallowRender();

  wrapper.unmount();
  expect(removeSideBarClass).toHaveBeenCalled();
  expect(removeWhitePageClass).toHaveBeenCalled();
  expect(removeEventListenerSpy).toHaveBeenCalledTimes(2);

  removeEventListenerSpy.mockReset();
});

it('should be able to bulk change specific issues', async () => {
  const wrapper = shallowRender({ currentUser: mockLoggedInUser() });
  await waitAndUpdate(wrapper);

  const instance = wrapper.instance();
  expect(wrapper.state().checked.length).toBe(0);
  instance.handleIssueCheck('foo');
  instance.handleIssueCheck('bar');
  expect(wrapper.state().checked.length).toBe(2);

  instance.handleOpenBulkChange();
  wrapper.update();
  expect(wrapper.find(BulkChangeModal).exists()).toBe(true);
  const { issues } = await wrapper.find(BulkChangeModal).props().fetchIssues({});
  expect(issues).toHaveLength(2);
});

it('should display the right facets open', () => {
  expect(
    shallowRender({
      location: mockLocation({ query: { types: 'BUGS' } }),
    }).state('openFacets')
  ).toEqual({
    owaspTop10: false,
    'owaspTop10-2021': false,
    sansTop25: false,
    severities: true,
    standards: false,
    sonarsourceSecurity: false,
    types: true,
  });
  expect(
    shallowRender({
      location: mockLocation({ query: { owaspTop10: 'a1' } }),
    }).state('openFacets')
  ).toEqual({
    owaspTop10: true,
    'owaspTop10-2021': false,
    sansTop25: false,
    severities: true,
    standards: true,
    sonarsourceSecurity: false,
    types: true,
  });
});

it('should correctly handle filter changes', () => {
  const push = jest.fn();
  const instance = shallowRender({ router: mockRouter({ push }) }).instance();
  instance.setState({ openFacets: { types: true } });
  instance.handleFilterChange({ types: ['VULNERABILITY'] });
  expect(instance.state.openFacets).toEqual({
    types: true,
    sonarsourceSecurity: true,
    standards: true,
  });
  expect(push).toHaveBeenCalled();
  instance.handleFilterChange({ types: ['BUGS'] });
  expect(instance.state.openFacets).toEqual({
    types: true,
    sonarsourceSecurity: true,
    standards: true,
  });
});

it('should fetch issues until defined', async () => {
  (searchIssues as jest.Mock).mockImplementation(mockSearchIssuesResponse());

  const mockDone = (_: Issue[], paging: Paging) =>
    paging.total <= paging.pageIndex * paging.pageSize;

  const wrapper = shallowRender({
    location: mockLocation({
      query: { open: '0' },
    }),
  });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  const result = await instance.fetchIssuesUntil(1, mockDone);
  expect(result.issues).toHaveLength(6);
  expect(result.paging.pageIndex).toBe(3);
});

describe('keydown event handler', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  jest.spyOn(instance, 'setState');

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('should handle alt', () => {
    instance.handleKeyDown(mockEvent({ key: KeyboardKeys.Alt }));
    expect(instance.setState).toHaveBeenCalledWith(enableLocationsNavigator);
  });
  it('should handle alt+↓', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.DownArrow }));
    expect(instance.setState).toHaveBeenCalledWith(selectNextLocation);
  });
  it('should handle alt+↑', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.UpArrow }));
    expect(instance.setState).toHaveBeenCalledWith(selectPreviousLocation);
  });
  it('should handle alt+←', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.LeftArrow }));
    expect(instance.setState).toHaveBeenCalledWith(selectPreviousFlow);
  });
  it('should handle alt+→', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, key: KeyboardKeys.RightArrow }));
    expect(instance.setState).toHaveBeenCalledWith(selectNextFlow);
  });
  it('should ignore if modal is open', () => {
    wrapper.setState({ bulkChangeModal: true });
    instance.handleKeyDown(mockEvent({ key: KeyboardKeys.Alt }));
    expect(instance.setState).not.toHaveBeenCalled();
  });
});

describe('keyup event handler', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  jest.spyOn(instance, 'setState');

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('should handle alt', () => {
    instance.handleKeyUp(mockEvent({ key: KeyboardKeys.Alt }));
    expect(instance.setState).toHaveBeenCalledWith(disableLocationsNavigator);
  });
});

it('should fetch more issues', async () => {
  (searchIssues as jest.Mock).mockImplementation(mockSearchIssuesResponse());
  const wrapper = shallowRender({});
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  await instance.fetchMoreIssues();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('issues')).toHaveLength(4);
});

it('should refresh branch status if issues are updated', async () => {
  const fetchBranchStatus = jest.fn();
  const branchLike = mockPullRequest();
  const component = mockComponent();
  const wrapper = shallowRender({ branchLike, component, fetchBranchStatus });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  const updatedIssue: Issue = { ...ISSUES[0], type: 'SECURITY_HOTSPOT' };
  instance.handleIssueChange(updatedIssue);
  expect(wrapper.state().issues[0].type).toEqual(updatedIssue.type);
  expect(fetchBranchStatus).toHaveBeenCalledWith(branchLike, component.key);

  fetchBranchStatus.mockClear();
  instance.handleBulkChangeDone();
  expect(fetchBranchStatus).toHaveBeenCalled();
});

it('should update the open issue when it is changed', async () => {
  (searchIssues as jest.Mock).mockImplementation(mockSearchIssuesResponse());

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  const issue = wrapper.state().issues[0];

  wrapper.setProps({ location: mockLocation({ query: { open: issue.key } }) });
  await waitAndUpdate(wrapper);

  expect(wrapper.state().openIssue).toEqual(issue);

  const updatedIssue: Issue = { ...issue, type: 'SECURITY_HOTSPOT' };
  wrapper.instance().handleIssueChange(updatedIssue);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().openIssue).toEqual(updatedIssue);
});

it('should handle createAfter query param with time', async () => {
  (searchIssues as jest.Mock).mockImplementation(mockSearchIssuesResponse());

  const wrapper = shallowRender({
    location: mockLocation({ query: { createdAfter: '2020-10-21' } }),
  });
  expect(wrapper.instance().createdAfterIncludesTime()).toBe(false);
  await waitAndUpdate(wrapper);

  wrapper.setProps({ location: mockLocation({ query: { createdAfter: '2020-10-21T17:21:00Z' } }) });
  expect(wrapper.instance().createdAfterIncludesTime()).toBe(true);

  (searchIssues as jest.Mock).mockClear();

  wrapper.instance().fetchIssues({});
  expect(searchIssues).toHaveBeenCalledWith(
    expect.objectContaining({ createdAfter: '2020-10-21T17:21:00+0000' })
  );
});

function mockSearchIssuesResponse(keyCount = 0, lineCount = 1) {
  return ({ p = 1 }) =>
    Promise.resolve({
      components: [referencedComponent],
      effortTotal: 1,
      facets: FACETS,
      issues: [
        mockRawIssue(false, {
          key: `${keyCount++}`,
          textRange: {
            startLine: lineCount++,
            endLine: lineCount,
            startOffset: 0,
            endOffset: 15,
          },
        }),
        mockRawIssue(false, {
          key: `${keyCount}`,
          textRange: {
            startLine: lineCount++,
            endLine: lineCount,
            startOffset: 0,
            endOffset: 15,
          },
        }),
      ],
      languages: [],
      paging: { pageIndex: p, pageSize: 2, total: 6 },
      rules: [],
      users: [],
    });
}

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      component={{
        breadcrumbs: [],
        key: 'foo',
        name: 'bar',
        qualifier: 'Doe',
      }}
      currentUser={mockLoggedInUser()}
      fetchBranchStatus={jest.fn()}
      location={mockLocation({ pathname: '/issues', query: {} })}
      router={mockRouter()}
      {...props}
    />
  );
}
