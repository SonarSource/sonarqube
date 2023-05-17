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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { Route } from 'react-router-dom';
import { byDisplayValue, byRole, byTestId, byText } from 'testing-library-selector';
import CodingRulesServiceMock from '../../../api/mocks/CodingRulesServiceMock';
import SecurityHotspotServiceMock from '../../../api/mocks/SecurityHotspotServiceMock';
import { getSecurityHotspots, setSecurityHotspotStatus } from '../../../api/security-hotspots';
import { searchUsers } from '../../../api/users';
import { mockBranch, mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { ComponentContextShape } from '../../../types/component';
import SecurityHotspotsApp from '../SecurityHotspotsApp';

jest.mock('../../../api/measures');
jest.mock('../../../api/security-hotspots');
jest.mock('../../../api/components');
jest.mock('../../../helpers/security-standard');
jest.mock('../../../api/users');

jest.mock('../../../api/rules');
jest.mock('../../../api/quality-profiles');
jest.mock('../../../api/issues');

const ui = {
  inputAssignee: byRole('combobox', { name: 'search.search_for_users' }),
  filterAssigneeToMe: byRole('checkbox', {
    name: 'hotspot.filters.assignee.assigned_to_me',
  }),
  clearFilters: byRole('menuitem', { name: 'hotspot.filters.clear' }),
  filterDropdown: byRole('button', { name: 'hotspot.filters.title' }),
  filterToReview: byRole('radio', { name: 'hotspot.filters.status.to_review' }),
  filterByStatus: byRole('combobox', { name: 'hotspot.filters.status' }),
  filterByPeriod: byRole('combobox', { name: 'hotspot.filters.period' }),
  filterNewCode: byRole('checkbox', { name: 'hotspot.filters.period.since_leak_period' }),
  noHotspotForFilter: byText('hotspots.no_hotspots_for_filters.title'),
  reviewButton: byRole('button', { name: 'hotspots.status.review' }),
  toReviewStatus: byText('hotspots.status_option.TO_REVIEW'),
  changeStatus: byRole('button', { name: 'hotspots.status.change_status' }),
  hotspotTitle: (name: string | RegExp) => byRole('heading', { name }),
  hotspotStatus: byRole('heading', { name: 'status: hotspots.status_option.FIXED' }),
  hotpostListTitle: byText('hotspots.list_title'),
  hotspotCommentBox: byRole('textbox', { name: 'hotspots.comment.field' }),
  commentSubmitButton: byRole('button', { name: 'hotspots.comment.submit' }),
  commentEditButton: byRole('button', { name: 'issue.comment.edit' }),
  commentDeleteButton: byRole('button', { name: 'issue.comment.delete' }),
  textboxWithText: (value: string) => byDisplayValue(value),
  activeAssignee: byRole('combobox', { name: 'hotspots.assignee.change_user' }),
  successGlobalMessage: byTestId('global-message__SUCCESS'),
  currentUserSelectionItem: byText('foo'),
  panel: byTestId('security-hotspot-test'),
  codeTab: byRole('tab', { name: 'hotspots.tabs.code' }),
  codeContent: byRole('table'),
  riskTab: byRole('tab', { name: 'hotspots.tabs.risk_description' }),
  riskContent: byText('Root cause'),
  vulnerabilityTab: byRole('tab', { name: 'hotspots.tabs.vulnerability_description' }),
  vulnerabilityContent: byText('Assess'),
  fixTab: byRole('tab', { name: 'hotspots.tabs.fix_recommendations' }),
  fixContent: byText('This is how to fix'),
  showAllHotspotLink: byRole('link', { name: 'hotspot.filters.show_all' }),
  activityTab: byRole('tab', { name: 'hotspots.tabs.activity' }),
  addCommentButton: byRole('button', { name: 'hotspots.status.add_comment' }),
};

const hotspotsHandler = new SecurityHotspotServiceMock();
const rulesHandles = new CodingRulesServiceMock();

afterEach(() => {
  hotspotsHandler.reset();
  rulesHandles.reset();
});

describe('rendering', () => {
  it('should render code variants correctly', async () => {
    renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-2'
    );
    expect(await screen.findAllByText('variant 1, variant 2')).toHaveLength(2);
  });

  it('should render the simple list when a file is selected', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp(
      `security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&files=src%2Findex.js`
    );

    expect(ui.filterDropdown.query()).not.toBeInTheDocument();
    expect(ui.filterToReview.query()).not.toBeInTheDocument();

    // Drop selection
    await user.click(ui.showAllHotspotLink.get());

    expect(ui.filterDropdown.get()).toBeInTheDocument();
    expect(ui.filterToReview.get()).toBeInTheDocument();
  });
});

it('should navigate when comming from SonarLint', async () => {
  // On main branch
  const rtl = renderSecurityHotspotsApp(
    'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-1'
  );

  expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();

  // On specific branch
  rtl.unmount();
  renderSecurityHotspotsApp(
    'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=b1-test-1&branch=b1',
    { branchLike: mockBranch({ name: 'b1' }) }
  );

  expect(await ui.hotspotTitle(/'F' is a magic number./).find()).toBeInTheDocument();
});

describe('CRUD', () => {
  it('should be able to self-assign a hotspot', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    expect(await ui.activeAssignee.find()).toHaveTextContent('John Doe');

    await user.click(ui.activeAssignee.get());
    await user.click(ui.currentUserSelectionItem.get());

    expect(ui.successGlobalMessage.get()).toHaveTextContent(`hotspots.assign.success.foo`);
    expect(ui.activeAssignee.get()).toHaveTextContent('foo');
  });

  it('should be able to search for a user on the assignee', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    await user.click(await ui.activeAssignee.find());
    await user.click(ui.inputAssignee.get());

    await act(async () => {
      await user.keyboard('User');
    });

    expect(searchUsers).toHaveBeenLastCalledWith({ q: 'User' });
    await user.keyboard('{Enter}');
    expect(ui.successGlobalMessage.get()).toHaveTextContent(`hotspots.assign.success.User John`);
  });

  it('should be able to change the status of a hotspot', async () => {
    const user = userEvent.setup();
    const comment = 'COMMENT-TEXT';

    renderSecurityHotspotsApp();

    expect(await ui.reviewButton.find()).toBeInTheDocument();

    await user.click(ui.reviewButton.get());
    await user.click(ui.toReviewStatus.get());

    await user.click(screen.getByRole('textbox', { name: 'hotspots.status.add_comment_optional' }));
    await user.keyboard(comment);

    await act(async () => {
      await user.click(ui.changeStatus.get());
    });

    await user.click(ui.activityTab.get());
    expect(setSecurityHotspotStatus).toHaveBeenLastCalledWith('test-1', {
      comment: 'COMMENT-TEXT',
      resolution: undefined,
      status: 'TO_REVIEW',
    });

    expect(ui.hotspotStatus.get()).toBeInTheDocument();
  });

  it('should not be able to change the status if does not have edit permissions', async () => {
    hotspotsHandler.setHotspotChangeStatusPermission(false);
    renderSecurityHotspotsApp();
    expect(await ui.reviewButton.find()).toBeDisabled();
  });

  it('should be able to add, edit and remove own comments', async () => {
    const uiComment = {
      saveButton: byRole('button', { name: 'hotspots.comment.submit' }),
      deleteButton: byRole('button', { name: 'delete' }),
    };
    const user = userEvent.setup();
    const comment = 'This is a comment from john doe';
    renderSecurityHotspotsApp();

    await user.click(await ui.activityTab.find());
    await user.click(ui.addCommentButton.get());

    const commentSection = ui.hotspotCommentBox.get();
    const submitButton = ui.commentSubmitButton.get();

    // Add a new comment
    await user.click(commentSection);
    await user.keyboard(comment);
    await user.click(submitButton);

    expect(await screen.findByText(comment)).toBeInTheDocument();

    // Edit the comment
    await user.click(ui.commentEditButton.get());
    await user.click(ui.textboxWithText(comment).get());
    await user.keyboard(' test');
    await user.click(uiComment.saveButton.get());

    expect(await byText(`${comment} test`).find()).toBeInTheDocument();

    // Delete the comment
    await user.click(ui.commentDeleteButton.get());
    await user.click(uiComment.deleteButton.get());

    expect(screen.queryByText(`${comment} test`)).not.toBeInTheDocument();
  });
});

describe('navigation', () => {
  it('should correctly handle tabs', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    await user.click(await ui.riskTab.find());
    expect(ui.riskContent.get()).toBeInTheDocument();

    await user.click(ui.vulnerabilityTab.get());
    expect(ui.vulnerabilityContent.get()).toBeInTheDocument();

    await user.click(ui.fixTab.get());
    expect(ui.fixContent.get()).toBeInTheDocument();

    await user.click(ui.codeTab.get());
    expect(ui.codeContent.get()).toHaveClass('source-table');
  });

  it('should be able to navigate the hotspot list with keyboard', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    await user.keyboard('{ArrowDown}');
    expect(await ui.hotspotTitle(/'2' is a magic number./).find()).toBeInTheDocument();
    await user.keyboard('{ArrowUp}');
    expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();
  });

  it('should navigate when coming from SonarLint', async () => {
    // On main branch
    const rtl = renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-1'
    );

    expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();

    // On specific branch
    rtl.unmount();
    renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=b1-test-1&branch=b1',
      { branchLike: mockBranch({ name: 'b1' }) }
    );

    expect(await ui.hotspotTitle(/'F' is a magic number./).find()).toBeInTheDocument();
  });
});

it('should be able to filter the hotspot list', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  expect(await ui.hotpostListTitle.find()).toBeInTheDocument();

  await user.click(ui.filterDropdown.get());
  await user.click(ui.filterAssigneeToMe.get());
  expect(ui.noHotspotForFilter.get()).toBeInTheDocument();

  await user.click(ui.filterToReview.get());

  expect(getSecurityHotspots).toHaveBeenLastCalledWith({
    inNewCodePeriod: false,
    onlyMine: true,
    p: 1,
    projectKey: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
    ps: 500,
    resolution: undefined,
    status: 'TO_REVIEW',
  });

  await user.click(ui.filterDropdown.get());
  await user.click(ui.filterNewCode.get());

  expect(getSecurityHotspots).toHaveBeenLastCalledWith({
    inNewCodePeriod: true,
    onlyMine: true,
    p: 1,
    projectKey: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
    ps: 500,
    resolution: undefined,
    status: 'TO_REVIEW',
  });

  await user.click(ui.filterDropdown.get());
  await user.click(ui.clearFilters.get());

  expect(ui.hotpostListTitle.get()).toBeInTheDocument();
});

function renderSecurityHotspotsApp(
  navigateTo?: string,
  component?: Partial<ComponentContextShape>
) {
  return renderAppWithComponentContext(
    'security_hotspots',
    () => <Route path="security_hotspots" element={<SecurityHotspotsApp />} />,
    {
      navigateTo,
      currentUser: mockLoggedInUser({
        login: 'foo',
        name: 'foo',
      }),
    },
    {
      branchLike: mockMainBranch(),
      onBranchesChange: jest.fn(),
      onComponentChange: jest.fn(),
      component: mockComponent({
        key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
        name: 'benflix',
      }),
      ...component,
    }
  );
}
