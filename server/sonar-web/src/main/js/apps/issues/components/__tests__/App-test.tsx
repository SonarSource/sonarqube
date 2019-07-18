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
import handleRequiredAuthentication from 'sonar-ui-common/helpers/handleRequiredAuthentication';
import { KEYCODE_MAP, keydown, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  mockComponent,
  mockCurrentUser,
  mockEvent,
  mockIssue,
  mockLocation,
  mockLoggedInUser,
  mockPullRequest,
  mockRouter
} from '../../../../helpers/testMocks';
import {
  enableLocationsNavigator,
  selectNextFlow,
  selectNextLocation,
  selectPreviousFlow,
  selectPreviousLocation
} from '../../actions';
import { App } from '../App';

jest.mock('sonar-ui-common/helpers/handleRequiredAuthentication', () => ({
  default: jest.fn()
}));

jest.mock('keymaster', () => {
  const key: any = (bindKey: string, _: string, callback: Function) => {
    document.addEventListener('keydown', (event: KeyboardEvent) => {
      if (bindKey.split(',').includes(KEYCODE_MAP[event.keyCode])) {
        return callback();
      }
      return true;
    });
  };

  key.getScope = () => 'issues';
  key.setScope = () => {};
  key.deleteScope = () => {};

  return key;
});

const ISSUES = [
  mockIssue(false, { key: 'foo' }),
  mockIssue(false, { key: 'bar' }),
  mockIssue(true, { key: 'third' }),
  mockIssue(false, { key: 'fourth' })
];
const FACETS = [{ property: 'severities', values: [{ val: 'MINOR', count: 4 }] }];
const PAGING = { pageIndex: 1, pageSize: 100, total: 4 };

const referencedComponent = { key: 'foo-key', name: 'bar', organization: 'John', uuid: 'foo-uuid' };

it('should render a list of issue', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);
  expect(wrapper.state().referencedComponentsById).toEqual({ 'foo-uuid': referencedComponent });
  expect(wrapper.state().referencedComponentsByKey).toEqual({ 'foo-key': referencedComponent });
});

it('should not render for anonymous user', () => {
  shallowRender({
    currentUser: mockCurrentUser({ isLoggedIn: false }),
    myIssues: true
  });
  expect(handleRequiredAuthentication).toBeCalled();
});

it('should open standard facets for vulnerabilities and hotspots', () => {
  const wrapper = shallowRender({
    location: { pathname: '/issues', query: { types: 'VULNERABILITY' } }
  });
  const instance = wrapper.instance();
  const fetchFacet = jest.spyOn(instance, 'fetchFacet');

  expect(wrapper.state('openFacets').standards).toEqual(true);
  expect(wrapper.state('openFacets').sonarsourceSecurity).toEqual(true);

  instance.handleFacetToggle('standards');
  expect(wrapper.state('openFacets').standards).toEqual(false);
  expect(fetchFacet).not.toBeCalled();

  instance.handleFacetToggle('standards');
  expect(wrapper.state('openFacets').standards).toEqual(true);
  expect(wrapper.state('openFacets').sonarsourceSecurity).toEqual(true);
  expect(fetchFacet).lastCalledWith('sonarsourceSecurity');

  instance.handleFacetToggle('owaspTop10');
  expect(wrapper.state('openFacets').owaspTop10).toEqual(true);
  expect(fetchFacet).lastCalledWith('owaspTop10');
});

it('should switch to source view if an issue is selected', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.setProps({ location: mockLocation({ query: { open: 'third' } }) });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should correctly bind key events for issue navigation', async () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });
  await waitAndUpdate(wrapper);

  expect(wrapper.state('selected')).toBe(ISSUES[0].key);

  keydown('down');
  expect(wrapper.state('selected')).toBe(ISSUES[1].key);

  keydown('up');
  keydown('up');
  expect(wrapper.state('selected')).toBe(ISSUES[0].key);

  keydown('down');
  keydown('down');
  keydown('down');
  keydown('down');
  keydown('down');
  keydown('down');
  expect(wrapper.state('selected')).toBe(ISSUES[3].key);

  keydown('right');
  expect(push).toBeCalledTimes(1);

  keydown('left');
  expect(push).toBeCalledTimes(2);
});

it('should be able to uncheck all issue with global checkbox', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  const instance = wrapper.instance();
  instance.handleIssueCheck('foo');
  instance.handleIssueCheck('bar');
  expect(wrapper.state().checked.length).toBe(2);

  instance.handleCheckAll(false);
  expect(wrapper.state().checked.length).toBe(0);
});

it('should be able to check all issue with global checkbox', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  const instance = wrapper.instance();
  expect(wrapper.state().checked.length).toBe(0);
  instance.handleCheckAll(true);
  expect(wrapper.state().checked.length).toBe(wrapper.state().issues.length);
});

it('should check all issues, even the ones that are not visible', async () => {
  const wrapper = shallowRender({
    fetchIssues: jest.fn().mockResolvedValue({
      components: [referencedComponent],
      effortTotal: 1,
      facets: FACETS,
      issues: ISSUES,
      languages: [],
      paging: { pageIndex: 1, pageSize: 100, total: 250 },
      rules: [],
      users: []
    })
  });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  // Checking all issues should show the correct count in the Bulk Change button.
  instance.handleCheckAll(true);
  waitAndUpdate(wrapper);
  expect(wrapper.find('#issues-bulk-change')).toMatchSnapshot();
});

it('should check max 500 issues', async () => {
  const wrapper = shallowRender({
    fetchIssues: jest.fn().mockResolvedValue({
      components: [referencedComponent],
      effortTotal: 1,
      facets: FACETS,
      issues: ISSUES,
      languages: [],
      paging: { pageIndex: 1, pageSize: 100, total: 1000 },
      rules: [],
      users: []
    })
  });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  // Checking all issues should show 500 in the Bulk Change button, and display
  // a warning.
  instance.handleCheckAll(true);
  waitAndUpdate(wrapper);
  expect(wrapper.find('#issues-bulk-change')).toMatchSnapshot();
});

it('should fetch issues for component', async () => {
  const wrapper = shallowRender({
    fetchIssues: fetchIssuesMockFactory(),
    location: mockLocation({
      query: { open: '0' }
    })
  });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('issues')).toHaveLength(2);

  await instance.fetchIssuesForComponent('', 0, 30);
  expect(wrapper.state('issues')).toHaveLength(6);
});

it('should display the right facets open', () => {
  expect(
    shallowRender({
      location: mockLocation({ query: { types: 'SECURITY_HOTSPOT' } })
    }).state('openFacets')
  ).toEqual({
    owaspTop10: false,
    sansTop25: false,
    severities: false,
    standards: true,
    sonarsourceSecurity: true,
    types: true
  });
  expect(
    shallowRender({
      location: mockLocation({ query: { types: 'BUGS,SECURITY_HOTSPOT' } })
    }).state('openFacets')
  ).toEqual({
    owaspTop10: false,
    sansTop25: false,
    severities: true,
    standards: true,
    sonarsourceSecurity: true,
    types: true
  });
  expect(
    shallowRender({
      location: mockLocation({ query: { owaspTop10: 'a1' } })
    }).state('openFacets')
  ).toEqual({
    owaspTop10: true,
    sansTop25: false,
    severities: true,
    standards: true,
    sonarsourceSecurity: false,
    types: true
  });
});

it('should correctly handle filter changes', () => {
  const push = jest.fn();
  const instance = shallowRender({ router: mockRouter({ push }) }).instance();
  instance.setState({ openFacets: { types: true } });
  instance.handleFilterChange({ types: ['VULNERABILITY'] });
  expect(instance.state.openFacets).toEqual({
    types: true,
    severities: true,
    sonarsourceSecurity: true,
    standards: true
  });
  expect(push).toBeCalled();
  instance.handleFilterChange({ types: ['BUGS'] });
  expect(instance.state.openFacets).toEqual({
    types: true,
    severities: true,
    sonarsourceSecurity: true,
    standards: true
  });
  instance.setState({ openFacets: { types: true } });
  instance.handleFilterChange({ types: ['SECURITY_HOTSPOT'] });
  expect(instance.state.openFacets).toEqual({
    types: true,
    severities: false,
    sonarsourceSecurity: true,
    standards: true
  });
});

it('should fetch issues until defined', async () => {
  const mockDone = (_lastIssue: T.Issue, paging: T.Paging) =>
    paging.total <= paging.pageIndex * paging.pageSize;

  const wrapper = shallowRender({
    fetchIssues: fetchIssuesMockFactory(),
    location: mockLocation({
      query: { open: '0' }
    })
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
    instance.handleKeyDown(mockEvent({ keyCode: 18 }));
    expect(instance.setState).toHaveBeenCalledWith(enableLocationsNavigator);
  });
  it('should handle alt+↓', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, keyCode: 40 }));
    expect(instance.setState).toHaveBeenCalledWith(selectNextLocation);
  });
  it('should handle alt+↑', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, keyCode: 38 }));
    expect(instance.setState).toHaveBeenCalledWith(selectPreviousLocation);
  });
  it('should handle alt+←', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, keyCode: 37 }));
    expect(instance.setState).toHaveBeenCalledWith(selectPreviousFlow);
  });
  it('should handle alt+→', () => {
    instance.handleKeyDown(mockEvent({ altKey: true, keyCode: 39 }));
    expect(instance.setState).toHaveBeenCalledWith(selectNextFlow);
  });
});

it('should fetch more issues', async () => {
  const wrapper = shallowRender({
    fetchIssues: fetchIssuesMockFactory()
  });
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

  const updatedIssue: T.Issue = { ...ISSUES[0], type: 'SECURITY_HOTSPOT' };
  instance.handleIssueChange(updatedIssue);
  expect(wrapper.state('issues')).toEqual([updatedIssue, ISSUES[1], ISSUES[2], ISSUES[3]]);
  expect(fetchBranchStatus).toBeCalledWith(branchLike, component.key);

  fetchBranchStatus.mockClear();
  instance.handleBulkChangeDone();
  expect(fetchBranchStatus).toBeCalled();

  fetchBranchStatus.mockClear();
  instance.handleReload();
  expect(fetchBranchStatus).toBeCalled();
});

function fetchIssuesMockFactory(keyCount = 0, lineCount = 1) {
  return jest.fn().mockImplementation(({ p }: any) =>
    Promise.resolve({
      components: [referencedComponent],
      effortTotal: 1,
      facets: FACETS,
      issues: [
        mockIssue(false, {
          key: '' + keyCount++,
          textRange: {
            startLine: lineCount++,
            endLine: lineCount,
            startOffset: 0,
            endOffset: 15
          }
        }),
        mockIssue(false, {
          key: '' + keyCount++,
          textRange: {
            startLine: lineCount++,
            endLine: lineCount,
            startOffset: 0,
            endOffset: 15
          }
        })
      ],
      languages: [],
      paging: { pageIndex: p || 1, pageSize: 2, total: 6 },
      rules: [],
      users: []
    })
  );
}

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      component={{
        breadcrumbs: [],
        key: 'foo',
        name: 'bar',
        organization: 'John',
        qualifier: 'Doe'
      }}
      currentUser={mockLoggedInUser()}
      fetchBranchStatus={jest.fn()}
      fetchIssues={jest.fn().mockResolvedValue({
        components: [referencedComponent],
        effortTotal: 1,
        facets: FACETS,
        issues: ISSUES,
        languages: [],
        paging: PAGING,
        rules: [],
        users: []
      })}
      location={{ pathname: '/issues', query: {} }}
      onBranchesChange={() => {}}
      organization={{ key: 'foo' }}
      router={mockRouter()}
      userOrganizations={[]}
      {...props}
    />
  );
}
