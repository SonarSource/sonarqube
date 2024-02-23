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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { mockFlowLocation, mockIssue, mockPaging } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../helpers/testSelector';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { FlowType, Issue } from '../../../../types/types';
import { VISIBLE_LOCATIONS_COLLAPSE } from '../IssueLocationsCrossFile';
import SubnavigationIssuesList from '../SubnavigationIssuesList';

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
  mockIssue(false, {
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
        mockIssue(false, {
          key: 'custom',
          message: 'Custom Issue',
          flows: Array.from({ length: VISIBLE_LOCATIONS_COLLAPSE }).map((i) => [
            mockFlowLocation({ component: `component-${i}` }),
          ]),
        }),
      ],
      {
        selected: 'custom',
      },
    );

    expect(ui.expandBadgesButton.query()).not.toBeInTheDocument();
  });
});

describe('interacting', () => {
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
    const flow = Array.from({ length: VISIBLE_LOCATIONS_COLLAPSE + 1 }).map((_, i) =>
      mockFlowLocation({
        component: `component-${i}`,
        index: i,
        msg: `loc ${i}`,
      }),
    );

    renderConciseIssues(
      [
        mockIssue(false, {
          key: 'custom',
          component: 'issue-component',
          message: 'Custom Issue',
          flows: [flow],
        }),
      ],
      {
        selected: 'custom',
        selectedFlowIndex: 0,
      },
    );

    expect(ui.expandBadgesButton.get()).toBeInTheDocument();
    expect(screen.getAllByText(/loc \d/)).toHaveLength(VISIBLE_LOCATIONS_COLLAPSE);
    await ui.clickExpandBadgesButton();

    expect(ui.expandBadgesButton.query()).not.toBeInTheDocument();
    expect(screen.getAllByText(/loc \d/)).toHaveLength(VISIBLE_LOCATIONS_COLLAPSE + 1);
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
        mockIssue(false, {
          key: 'custom',
          message: 'Custom Issue',
          secondaryLocations: [],
          flows: [[loc], [loc, loc, loc], [loc]],
        }),
      ],
      {
        onFlowSelect,
        selected: 'custom',
      },
    );

    expect(onFlowSelect).not.toHaveBeenCalled();

    await user.click(screen.getByText('issue.flow.x_steps.3'));
    expect(onFlowSelect).toHaveBeenCalledTimes(1);
    expect(onFlowSelect).toHaveBeenLastCalledWith(1);
  });
});

function getPageObject() {
  const selectors = {
    headerBackButton: byRole('link', { name: 'issues.return_to_list' }),
    expandBadgesButton: byRole('button', { name: /issues.show_x_more_locations.\d/ }),
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
  listProps: Partial<ComponentPropsType<typeof SubnavigationIssuesList>> = {},
) {
  const wrapper = renderComponent(
    <SubnavigationIssuesList
      fetchMoreIssues={jest.fn()}
      loading={false}
      loadingMore={false}
      paging={mockPaging({ total: 10 })}
      issues={issues}
      onFlowSelect={jest.fn()}
      onIssueSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      selected={undefined}
      selectedFlowIndex={undefined}
      selectedLocationIndex={undefined}
      {...listProps}
    />,
  );

  function override(
    issues: Issue[],
    listProps: Partial<ComponentPropsType<typeof SubnavigationIssuesList>> = {},
  ) {
    wrapper.rerender(
      <SubnavigationIssuesList
        fetchMoreIssues={jest.fn()}
        issues={issues}
        loading={false}
        loadingMore={false}
        paging={mockPaging({ total: 10 })}
        onFlowSelect={jest.fn()}
        onIssueSelect={jest.fn()}
        onLocationSelect={jest.fn()}
        selected={undefined}
        selectedFlowIndex={undefined}
        selectedLocationIndex={undefined}
        {...listProps}
      />,
    );
  }

  return { ...wrapper, override };
}
