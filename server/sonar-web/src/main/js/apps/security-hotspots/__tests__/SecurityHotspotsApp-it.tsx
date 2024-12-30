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

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route } from 'react-router-dom';
import { byDisplayValue, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import { MetricKey } from '~sonar-aligned/types/metrics';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import CodingRulesServiceMock from '../../../api/mocks/CodingRulesServiceMock';
import CveServiceMock from '../../../api/mocks/CveServiceMock';
import SecurityHotspotServiceMock from '../../../api/mocks/SecurityHotspotServiceMock';
import { getSecurityHotspots, setSecurityHotspotStatus } from '../../../api/security-hotspots';
import { getUsers } from '../../../api/users';
import { mockComponent } from '../../../helpers/mocks/component';
import { openHotspot, probeSonarLintServers } from '../../../helpers/sonarlint';
import { get, save } from '../../../helpers/storage';
import { mockCve, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { ComponentContextShape } from '../../../types/component';
import SecurityHotspotsApp from '../SecurityHotspotsApp';
import useStickyDetection from '../hooks/useStickyDetection';

jest.mock('../../../api/measures');
jest.mock('../../../api/security-hotspots');
jest.mock('../../../api/components');
jest.mock('../../../helpers/security-standard');
jest.mock('../../../api/users');

jest.mock('../../../api/rules');
jest.mock('../../../api/quality-profiles');
jest.mock('../../../api/issues');
jest.mock('../hooks/useStickyDetection');
jest.mock('../../../helpers/sonarlint', () => ({
  openHotspot: jest.fn().mockResolvedValue(null),
  probeSonarLintServers: jest.fn().mockResolvedValue([
    {
      description: 'I use VIM',
      ideName: 'VIM',
      port: 1234,
    },
  ]),
}));
jest.mock('.../../../helpers/storage');

const ui = {
  activeAssignee: byRole('combobox', { name: 'hotspots.assignee.change_user' }),
  activityTab: byRole('tab', { name: /hotspots.tabs.activity/ }),
  addCommentButton: byRole('button', { name: 'hotspots.status.add_comment' }),
  changeStatus: byRole('button', { name: 'hotspots.status.change_status' }),
  clearFilters: byRole('menuitem', { name: 'hotspot.filters.clear' }),
  codeContent: byRole('table'),
  codeTab: byRole('tab', { name: /hotspots.tabs.code/ }),
  commentDeleteButton: byRole('button', { name: 'issue.comment.delete' }),
  commentEditButton: byRole('button', { name: 'issue.comment.edit' }),
  commentSubmitButton: byRole('button', { name: 'hotspots.comment.submit' }),
  continueReviewingButton: byRole('button', { name: 'hotspots.continue_to_next_hotspot' }),
  currentUserSelectionItem: byText('foo'),
  dontShowSuccessDialogCheckbox: byRole('checkbox', {
    name: 'hotspots.success_dialog.do_not_show',
  }),
  filterAssigneeToMe: byRole('checkbox', {
    name: 'hotspot.filters.assignee.assigned_to_me',
  }),
  filterByPeriod: byRole('combobox', { name: 'hotspot.filters.period' }),
  filterByStatus: byRole('combobox', { name: 'hotspot.filters.status' }),
  filterDropdown: byRole('button', { name: 'hotspot.filters.title' }),
  filterNewCode: byRole('checkbox', { name: 'hotspot.filters.period.since_leak_period' }),
  filterToReview: byRole('radio', { name: 'hotspot.filters.status.to_review' }),
  fixContent: byText('This is how to fix'),
  fixTab: byRole('tab', { name: /hotspots.tabs.fix_recommendations/ }),
  cveTable: byRole('table', { name: 'rule.cve_details' }),
  hotpostListTitle: byText('hotspots.list_title'),
  hotspotCommentBox: byRole('textbox', { name: 'hotspots.comment.field' }),
  hotspotStatus: byRole('heading', { name: 'status: hotspots.status_option.FIXED' }),
  hotspotTitle: (name: string | RegExp) => byRole('heading', { name }),
  inputAssignee: byRole('searchbox', { name: 'search.search_for_users' }),
  noHotspotForFilter: byText('hotspots.no_hotspots_for_filters.title'),
  openInIDEButton: byRole('button', { name: 'open_in_ide' }),
  panel: byTestId('security-hotspot-test'),
  reviewButton: byRole('button', { name: 'hotspots.status.review' }),
  riskContent: byText('Root cause'),
  riskTab: byRole('tab', { name: /hotspots.tabs.risk_description/ }),
  seeStatusHotspots: byRole('button', { name: /hotspots.see_x_hotspots/ }),
  showAllHotspotLink: byRole('link', { name: 'hotspot.filters.show_all' }),
  successGlobalMessage: byTestId('global-message__SUCCESS'),
  textboxWithText: (value: string) => byDisplayValue(value),
  toReviewStatus: byText('hotspots.status_option.TO_REVIEW'),
  vulnerabilityContent: byText('Assess'),
  vulnerabilityTab: byRole('tab', { name: /hotspots.tabs.vulnerability_description/ }),
};

const originalScrollTo = window.scrollTo;
const hotspotsHandler = new SecurityHotspotServiceMock();
const cveHandler = new CveServiceMock();
const rulesHandles = new CodingRulesServiceMock();
const branchHandler = new BranchesServiceMock();

const mockComponentInstance = mockComponent({
  key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
  name: 'benflix',
});

let showDialog = 'true';

jest.mocked(save).mockImplementation((_key: string, value?: string) => {
  if (value) {
    showDialog = value;
  }
});

jest.mocked(get).mockImplementation(() => showDialog);

beforeAll(() => {
  Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: () => {
      /* noop */
    },
  });
});

afterAll(() => {
  Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: originalScrollTo,
  });
});

beforeEach(() => {
  jest.mocked(useStickyDetection).mockImplementation(() => false);
});

afterEach(() => {
  hotspotsHandler.reset();
  cveHandler.reset();
  rulesHandles.reset();
  branchHandler.reset();
});

describe('rendering', () => {
  it('should render code variants correctly', async () => {
    renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-2',
    );

    expect(await screen.findAllByText('variant 1, variant 2')).toHaveLength(2);
  });

  it('should render the simple list when a file is selected', async () => {
    const user = userEvent.setup();

    renderSecurityHotspotsApp(
      `security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&files=src%2Findex.js&cwe=foo&inNewCodePeriod=true`,
    );

    await waitFor(() => {
      expect(ui.filterDropdown.query()).not.toBeInTheDocument();
    });

    expect(ui.filterToReview.query()).not.toBeInTheDocument();

    // Drop selection
    await user.click(ui.showAllHotspotLink.get());

    expect(ui.filterDropdown.get()).toBeInTheDocument();
    expect(ui.filterToReview.get()).toBeInTheDocument();
  });

  it('should render hotspot header in sticky mode', async () => {
    jest.mocked(useStickyDetection).mockImplementation(() => true);

    renderSecurityHotspotsApp();

    expect(await ui.reviewButton.findAll()).toHaveLength(2);
  });

  it('should render CVE details', async () => {
    const user = userEvent.setup();

    renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-cve',
    );

    await user.click(await ui.riskTab.find());
    expect(await screen.findByRole('heading', { name: 'CVE-2021-12345' })).toBeInTheDocument();

    const rows = byRole('row').getAll(ui.cveTable.get());
    expect(rows).toHaveLength(4);
    expect(byText('CWE-79, CWE-89').get(rows[0])).toBeInTheDocument();
    expect(byText('rule.cve_details.epss_score.value.20.56').get(rows[1])).toBeInTheDocument();
    expect(byText('0.3').get(rows[2])).toBeInTheDocument();
    expect(byText('Oct 04, 2021').get(rows[3])).toBeInTheDocument();
  });

  it('should not render CVE CVSS and CWEs when not set', async () => {
    const user = userEvent.setup();
    cveHandler.setCveList([
      mockCve({
        cvssScore: undefined,
        cwes: [],
      }),
    ]);

    renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-cve',
    );

    await user.click(await ui.riskTab.find());
    expect(await screen.findByRole('heading', { name: 'CVE-2021-12345' })).toBeInTheDocument();

    const rows = byRole('row').getAll(ui.cveTable.get());
    expect(rows).toHaveLength(2);
    expect(byText('rule.cve_details.epss_score.value.20.56').get(rows[0])).toBeInTheDocument();
    expect(byText('Oct 04, 2021').get(rows[1])).toBeInTheDocument();
  });
});

describe('CRUD', () => {
  it('should be able to self-assign a hotspot', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    expect(await ui.activeAssignee.find()).toHaveTextContent('John Doe');

    await user.click(ui.activeAssignee.get());
    await user.click(ui.currentUserSelectionItem.get());

    expect(await ui.successGlobalMessage.find()).toHaveTextContent(`hotspots.assign.success.foo`);
    expect(ui.activeAssignee.get()).toHaveTextContent('foo');
  });

  it('should be able to search for a user on the assignee', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    await user.click(await ui.activeAssignee.find());
    await user.click(ui.inputAssignee.get());

    await user.keyboard('User');

    expect(getUsers).toHaveBeenLastCalledWith({ q: 'User' });
    await user.keyboard('{Enter}');
    expect(await ui.successGlobalMessage.find()).toHaveTextContent(
      `hotspots.assign.success.User John`,
    );
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

    await user.click(ui.changeStatus.get());

    expect(ui.continueReviewingButton.get()).toBeInTheDocument();
    await user.click(ui.continueReviewingButton.get());

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
    expect(ui.codeContent.get()).toBeInTheDocument();
  });

  it('should be able to navigate the hotspot list with keyboard', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();
    await user.keyboard('{ArrowDown}');
    expect(await ui.hotspotTitle(/'2' is a magic number./).find()).toBeInTheDocument();
    await user.keyboard('{ArrowUp}');
    expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();
  });

  it('should be able to navigate between tabs with keyboard', async () => {
    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    await user.keyboard('{ArrowLeft}');
    expect(await ui.codeContent.find()).toBeInTheDocument();

    await user.keyboard('{ArrowRight}');
    expect(ui.riskContent.get()).toBeInTheDocument();

    await user.keyboard('{ArrowRight}');
    expect(ui.vulnerabilityContent.get()).toBeInTheDocument();

    await user.keyboard('{ArrowRight}');
    expect(ui.fixContent.get()).toBeInTheDocument();

    await user.keyboard('{ArrowRight}');
    expect(ui.addCommentButton.get()).toBeInTheDocument();

    await user.keyboard('{ArrowRight}');
    expect(ui.addCommentButton.get()).toBeInTheDocument();
  });

  it('should navigate when coming from SonarLint', async () => {
    // On main branch
    const rtl = renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=test-1',
    );
    expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();

    // On specific branch
    rtl.unmount();
    renderSecurityHotspotsApp(
      'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed&hotspots=b1-test-1&branch=normal-branch',
    );
    expect(await ui.hotspotTitle(/'F' is a magic number./).find()).toBeInTheDocument();
  });

  it('should allow to open a hotspot in an IDE', async () => {
    const user = userEvent.setup();

    renderSecurityHotspotsApp();

    await user.click(await ui.openInIDEButton.find());
    expect(openHotspot).toHaveBeenCalledWith(1234, 'hotspot-component', 'test-1');
  });

  it('should allow to choose in which IDE to open a hotspot', async () => {
    jest.mocked(probeSonarLintServers).mockResolvedValueOnce([
      {
        port: 1234,
        ideName: 'VIM',
        description: 'I use VIM',
      },
      {
        port: 4567,
        ideName: 'MS Paint',
        description: 'I use MS Paint cuz Ima boss',
      },
    ]);

    const user = userEvent.setup();
    renderSecurityHotspotsApp();

    await user.click(await ui.openInIDEButton.find());
    await user.click(screen.getByRole('menuitem', { name: /MS Paint/ }));
    expect(openHotspot).toHaveBeenCalledWith(4567, 'hotspot-component', 'test-1');
  });
});

it('after status change, should be able to disable success dialog show', async () => {
  const user = userEvent.setup();

  renderSecurityHotspotsApp();
  await user.click(await ui.reviewButton.find());
  await user.click(ui.toReviewStatus.get());

  await user.click(ui.changeStatus.get());

  await user.click(ui.dontShowSuccessDialogCheckbox.get());
  expect(ui.dontShowSuccessDialogCheckbox.get()).toBeChecked();
  await user.click(ui.continueReviewingButton.get());

  // Repeat status change and verify that dialog is not shown
  await user.click(await ui.reviewButton.find());
  await user.click(ui.toReviewStatus.get());

  await user.click(ui.changeStatus.get());

  expect(ui.continueReviewingButton.query()).not.toBeInTheDocument();
});

it('should be able to filter the hotspot list', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  expect(await ui.hotpostListTitle.find()).toBeInTheDocument();

  await user.click(ui.filterDropdown.get());

  expect(ui.filterAssigneeToMe.get()).toBeEnabled();

  await user.click(ui.filterAssigneeToMe.get());

  // No results message + screen reader announcement
  expect(ui.noHotspotForFilter.getAll()).toHaveLength(2);

  await user.click(ui.filterToReview.get());

  expect(getSecurityHotspots).toHaveBeenLastCalledWith(
    {
      inNewCodePeriod: false,
      onlyMine: true,
      p: 1,
      project: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
      ps: 500,
      resolution: undefined,
      status: 'TO_REVIEW',
    },
    undefined,
  );

  await user.click(ui.filterDropdown.get());
  await user.click(await ui.filterNewCode.find());

  expect(getSecurityHotspots).toHaveBeenLastCalledWith(
    {
      inNewCodePeriod: true,
      onlyMine: true,
      p: 1,
      project: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
      ps: 500,
      resolution: undefined,
      status: 'TO_REVIEW',
    },
    undefined,
  );

  await user.click(ui.filterDropdown.get());
  await user.click(ui.clearFilters.get());

  expect(ui.hotpostListTitle.get()).toBeInTheDocument();
});

it('should disable the "assigned to me" filter if the project is indexing', async () => {
  const user = userEvent.setup();

  renderSecurityHotspotsApp(
    'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
    { component: { ...mockComponentInstance, needIssueSync: true } },
  );

  await user.click(ui.filterDropdown.get());

  expect(ui.filterAssigneeToMe.get()).toHaveAttribute('disabled');
});

function renderSecurityHotspotsApp(
  navigateTo?: string,
  component?: Partial<ComponentContextShape>,
) {
  return renderAppWithComponentContext(
    MetricKey.security_hotspots,
    () => <Route path={MetricKey.security_hotspots} element={<SecurityHotspotsApp />} />,
    {
      currentUser: mockLoggedInUser({
        login: 'foo',
        name: 'foo',
      }),
      navigateTo:
        navigateTo ??
        'security_hotspots?id=guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
    },
    {
      onComponentChange: jest.fn(),
      component: mockComponentInstance,
      ...component,
    },
  );
}
