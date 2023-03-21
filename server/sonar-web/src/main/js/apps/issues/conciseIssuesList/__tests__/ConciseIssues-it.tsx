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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from 'testing-library-selector';
import { mockFlowLocation, mockIssue } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { FlowType, Issue } from '../../../../types/types';
import { COLLAPSE_LIMIT } from '../ConciseIssueLocations';
import ConciseIssuesList, { ConciseIssuesListProps } from '../ConciseIssuesList';
import ConciseIssuesListHeader, { ConciseIssuesListHeaderProps } from '../ConciseIssuesListHeader';

const loc = mockFlowLocation();
const issues = [
  mockIssue(false, {
    key: 'issue1',
    message: 'Issue 1',
    component: 'foo',
    componentLongName: 'Long Foo',
  }),
  mockIssue(false, {
    key: 'issue2',
    message: 'Issue 2',
    component: 'foo',
    componentLongName: 'Long Foo',
  }),
  mockIssue(false, {
    key: 'issue3',
    message: 'Issue 3',
    component: 'bar',
    componentLongName: 'Long Bar',
  }),
  mockIssue(true, {
    key: 'issue4',
    message: 'Issue 4',
    component: 'foo',
    componentLongName: 'Long Foo',
    flowsWithType: [
      {
        type: FlowType.DATA,
        description: 'Flow Foo',
        locations: [mockFlowLocation({ msg: 'loc 1' })],
      },
    ],
  }),
];

const scrollTo = jest.fn();

beforeAll(() => {
  // eslint-disable-next-line testing-library/no-node-access
  document.querySelector = jest.fn(() => ({
    scrollTo,
    getBoundingClientRect: () => ({
      height: 10,
    }),
  }));
});

beforeEach(() => {
  scrollTo.mockClear();
});

describe('rendering', () => {
  it('should hide the back button', () => {
    const { ui } = getPageObject();
    renderConciseIssues(issues, {}, { displayBackButton: false });

    expect(ui.headerBackButton.query()).not.toBeInTheDocument();
  });

  it('should render concise issues without duplicating component', () => {
    renderConciseIssues(issues);

    expect(screen.getAllByTitle('Long Foo')).toHaveLength(2);
    expect(screen.getByTitle('Long Bar')).toBeInTheDocument();
  });

  it('should scroll issue into view when one of the issue is selected', () => {
    renderConciseIssues(issues, {
      selected: 'issue2',
    });

    expect(scrollTo).toHaveBeenCalledTimes(1);
  });

  it('should show locations and flows when selected', () => {
    renderConciseIssues(issues, {
      selected: 'issue4',
      selectedFlowIndex: 0,
    });

    expect(screen.getByText('Flow Foo')).toBeInTheDocument();
    expect(screen.getByText('loc 1')).toBeInTheDocument();
  });

  it('should hide locations and flows when not selected', () => {
    renderConciseIssues(issues, {
      selected: 'issue2',
    });

    expect(screen.queryByText('Flow Foo')).not.toBeInTheDocument();
    expect(screen.queryByText('loc 1')).not.toBeInTheDocument();
  });

  it('should not render the expand button if below the collapse limit', () => {
    const { ui } = getPageObject();
    renderConciseIssues(
      [
        ...issues,
        mockIssue(true, {
          key: 'custom',
          message: 'Custom Issue',
          flows: Array.from({ length: COLLAPSE_LIMIT - 1 }).map(() => [loc]),
          secondaryLocations: [loc, loc],
        }),
      ],
      {
        selected: 'custom',
      }
    );

    expect(ui.expandBadgesButton.query()).not.toBeInTheDocument();
  });
});

describe('interacting', () => {
  it('should handle back button properly', async () => {
    const { ui } = getPageObject();
    const onBackClick = jest.fn();
    const { override } = renderConciseIssues(
      issues,
      {},
      {
        displayBackButton: true,
        loading: true,
        onBackClick,
      }
    );

    // Back button should be shown, but disabled
    expect(ui.headerBackButton.get()).toBeInTheDocument();
    await ui.clickBackButton();
    expect(onBackClick).toHaveBeenCalledTimes(0);

    // Re-render without loading
    override(
      issues,
      {},
      {
        displayBackButton: true,
        loading: false,
        onBackClick,
      }
    );

    // Back button should be shown and enabled
    expect(ui.headerBackButton.get()).toBeInTheDocument();
    await ui.clickBackButton();
    expect(onBackClick).toHaveBeenCalledTimes(1);
  });

  it('should scroll selected issue into view', () => {
    const { override } = renderConciseIssues(issues, {
      selected: 'issue2',
    });

    expect(scrollTo).toHaveBeenCalledTimes(1);

    override(issues, {
      selected: 'issue4',
    });
    expect(scrollTo).toHaveBeenCalledTimes(2);
  });

  it('expand button should work correctly', async () => {
    const { ui } = getPageObject();
    renderConciseIssues(
      [
        ...issues,
        mockIssue(true, {
          key: 'custom',
          message: 'Custom Issue',
          flows: Array.from({ length: COLLAPSE_LIMIT }).map(() => [loc]),
          secondaryLocations: [loc, loc],
        }),
      ],
      {
        selected: 'custom',
      }
    );

    expect(ui.expandBadgesButton.get()).toBeInTheDocument();
    expect(ui.boxLocationFlowBadgeText.getAll()).toHaveLength(COLLAPSE_LIMIT - 1);
    await ui.clickExpandBadgesButton();

    expect(ui.expandBadgesButton.query()).not.toBeInTheDocument();
    expect(ui.boxLocationFlowBadgeText.getAll()).toHaveLength(9);
  });

  it('issue selection should correctly be handled', async () => {
    const { user } = getPageObject();
    const onIssueSelect = jest.fn();
    renderConciseIssues(issues, {
      onIssueSelect,
      selected: 'issue2',
    });

    expect(onIssueSelect).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Issue 4' }));
    expect(onIssueSelect).toHaveBeenCalledTimes(1);
    expect(onIssueSelect).toHaveBeenLastCalledWith('issue4');
  });

  it('flow selection should correctly be handled', async () => {
    const { user } = getPageObject();
    const onFlowSelect = jest.fn();
    renderConciseIssues(
      [
        ...issues,
        mockIssue(true, {
          key: 'custom',
          message: 'Custom Issue',
          secondaryLocations: [],
          flows: [[loc], [loc, loc, loc], [loc]],
        }),
      ],
      {
        onFlowSelect,
        selected: 'issue4',
      }
    );

    expect(onFlowSelect).not.toHaveBeenCalled();

    await user.click(screen.getByText('+3', { exact: false }));
    expect(onFlowSelect).toHaveBeenCalledTimes(1);
    expect(onFlowSelect).toHaveBeenLastCalledWith(1);
  });
});

function getPageObject() {
  const selectors = {
    headerBackButton: byRole('link', { name: 'issues.return_to_list' }),
    expandBadgesButton: byRole('button', { name: '...' }),
    boxLocationFlowBadgeText: byText('issue.this_issue_involves_x_code_locations', {
      exact: false,
    }),
  };
  const user = userEvent.setup();
  const ui = {
    ...selectors,
    async clickBackButton() {
      await user.click(ui.headerBackButton.get());
    },
    async clickExpandBadgesButton() {
      await user.click(ui.expandBadgesButton.get());
    },
  };
  return { ui, user };
}

function renderConciseIssues(
  issues: Issue[],
  listProps: Partial<ConciseIssuesListProps> = {},
  headerProps: Partial<ConciseIssuesListHeaderProps> = {}
) {
  const wrapper = renderComponent(
    <>
      <ConciseIssuesListHeader
        displayBackButton={false}
        loading={false}
        onBackClick={jest.fn()}
        {...headerProps}
      />
      <ConciseIssuesList
        issues={issues}
        onFlowSelect={jest.fn()}
        onIssueSelect={jest.fn()}
        onLocationSelect={jest.fn()}
        selected={undefined}
        selectedFlowIndex={undefined}
        selectedLocationIndex={undefined}
        {...listProps}
      />
    </>
  );

  function override(
    issues: Issue[],
    listProps: Partial<ConciseIssuesListProps> = {},
    headerProps: Partial<ConciseIssuesListHeaderProps> = {}
  ) {
    wrapper.rerender(
      <>
        <ConciseIssuesListHeader
          displayBackButton={false}
          loading={false}
          onBackClick={jest.fn()}
          {...headerProps}
        />
        <ConciseIssuesList
          issues={issues}
          onFlowSelect={jest.fn()}
          onIssueSelect={jest.fn()}
          onLocationSelect={jest.fn()}
          selected={undefined}
          selectedFlowIndex={undefined}
          selectedLocationIndex={undefined}
          {...listProps}
        />
      </>
    );
  }

  return { ...wrapper, override };
}
