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
import { omit, pick } from 'lodash';
import * as React from 'react';
import { Route } from 'react-router-dom';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { mockIssue, mockLoggedInUser, mockRawIssue } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../helpers/testSelector';
import { ComponentPropsType } from '../../../helpers/testUtils';
import {
  IssueActions,
  IssueSeverity,
  IssueStatus,
  IssueTransition,
  IssueType,
} from '../../../types/issues';
import { RestUserDetailed } from '../../../types/users';
import Issue from '../Issue';

jest.mock('../../../helpers/preferences', () => ({
  getKeyboardShortcutEnabled: jest.fn(() => true),
}));

const usersHandler = new UsersServiceMock();
const issuesHandler = new IssuesServiceMock(usersHandler);

beforeEach(() => {
  issuesHandler.reset();
  usersHandler.reset();
  usersHandler.users = [mockLoggedInUser() as unknown as RestUserDetailed];
});

describe('rendering', () => {
  it('should render correctly for issue message and effort', async () => {
    const { ui } = getPageObject();
    const issue = mockIssue(true, { effort: '2 days', message: 'This is an issue' });
    const onClick = jest.fn();
    renderIssue({ issue, onSelect: onClick });

    expect(ui.effort('2 days').get()).toBeInTheDocument();
    expect(ui.issueMessageLink.get()).toHaveAttribute(
      'href',
      '/issues?scopes=MAIN&impactSeverities=LOW&types=VULNERABILITY&open=AVsae-CQS-9G3txfbFN2',
    );

    await ui.clickIssueMessage();
    expect(onClick).toHaveBeenCalledWith(issue.key);
  });

  it('should render correctly for external rule engines', () => {
    renderIssue({ issue: mockIssue(true, { externalRuleEngine: 'ESLINT' }) });
    expect(screen.getByRole('status', { name: 'ESLINT' })).toBeInTheDocument();
  });

  it('should render the SonarLint icon correctly', async () => {
    renderIssue({ issue: mockIssue(false, { quickFixAvailable: true }) });
    await expect(
      screen.getByText('issue.quick_fix_available_with_sonarlint_no_link'),
    ).toHaveATooltipWithContent('issue.quick_fix_available_with_sonarlint');
  });

  it('should render correctly with a checkbox', async () => {
    const { ui } = getPageObject();
    const onCheck = jest.fn();
    const issue = mockIssue();
    renderIssue({ onCheck, issue });
    await ui.toggleCheckbox();
    expect(onCheck).toHaveBeenCalledWith(issue.key);
  });

  it('should correctly render any code variants', async () => {
    const { ui } = getPageObject();
    renderIssue({ issue: mockIssue(false, { codeVariants: ['variant 1', 'variant 2'] }) });
    await expect(ui.variants(2).get()).toHaveATooltipWithContent('variant 1, variant 2');
  });
});

describe('updating', () => {
  it('should allow updating the status', async () => {
    const { ui } = getPageObject();
    const issue = mockRawIssue(false, {
      issueStatus: IssueStatus.Open,
      transitions: [IssueTransition.Confirm, IssueTransition.UnConfirm],
    });
    issuesHandler.setIssueList([{ issue, snippets: {} }]);
    renderIssue({
      issue: mockIssue(false, { ...pick(issue, 'key', 'status', 'transitions') }),
    });

    await ui.updateStatus(IssueStatus.Open, IssueTransition.Confirm);
    expect(ui.updateStatusBtn(IssueStatus.Confirmed).get()).toBeInTheDocument();
  });

  it('should allow assigning', async () => {
    const { ui } = getPageObject();
    const issue = mockRawIssue(false, {
      assignee: 'leia',
      actions: [IssueActions.Assign],
    });
    issuesHandler.setIssueList([{ issue, snippets: {} }]);
    renderIssue({
      issue: mockIssue(false, { ...pick(issue, 'actions', 'key', 'assignee') }),
    });

    await ui.updateAssignee('leia', 'Skywalker');
    expect(ui.updateAssigneeBtn('luke').get()).toBeInTheDocument();
  });

  it('should allow updating the tags', async () => {
    const { ui } = getPageObject();
    const issue = mockRawIssue(false, {
      tags: [],
      actions: [IssueActions.SetTags],
    });
    issuesHandler.setIssueList([{ issue, snippets: {} }]);
    renderIssue({ issue: mockIssue(false, { ...pick(issue, 'actions', 'key', 'tags') }) });

    await ui.addTag('accessibility');
    await ui.addTag('android', ['accessibility']);
    expect(ui.updateTagsBtn(['accessibility', 'android']).get()).toBeInTheDocument();
  });
});

it('should correctly handle keyboard shortcuts', async () => {
  const { ui } = getPageObject();
  const onCheck = jest.fn();
  const issue = mockRawIssue(false, {
    actions: Object.values(IssueActions),
    assignee: 'luke',
    transitions: [IssueTransition.Confirm, IssueTransition.UnConfirm],
  });
  issuesHandler.setIssueList([{ issue, snippets: {} }]);
  usersHandler.setCurrentUser(mockLoggedInUser({ login: 'leia', name: 'Organa' }));
  renderIssue({
    onCheck,
    selected: true,
    issue: mockIssue(false, { ...pick(issue, 'actions', 'key', 'assignee', 'transitions') }),
  });

  await ui.pressTransitionShortcut();
  expect(ui.setStatusBtn(IssueTransition.UnConfirm).get()).toBeInTheDocument();
  await ui.pressDismissShortcut();

  await ui.pressAssignShortcut();
  expect(ui.setAssigneeBtn(/Organa/).get()).toBeInTheDocument();
  await ui.pressDismissShortcut();

  await ui.pressTagsShortcut();
  expect(ui.tagsSearchInput.get()).toBeInTheDocument();
  await ui.pressDismissShortcut();

  await ui.pressCheckShortcut();
  expect(onCheck).toHaveBeenCalled();

  expect(ui.updateAssigneeBtn('luke').get()).toBeInTheDocument();
  await ui.pressAssignToMeShortcut();
  expect(ui.updateAssigneeBtn('leia').get()).toBeInTheDocument();
});

function getPageObject() {
  const user = userEvent.setup();

  const selectors = {
    // Issue
    locationsBadge: (count: number) => byText(count),
    lineInfo: (line: number) => byText(`L${line}`),
    effort: (effort: string) => byText(`issue.x_effort.${effort}`),
    whyLink: byRole('link', { name: 'issue.why_this_issue.long' }),
    checkbox: byRole('checkbox'),
    issueMessageLink: byRole('link', { name: 'This is an issue' }),
    variants: (n: number) => byText(`issue.x_code_variants.${n}`),

    // Changelog
    toggleChangelogBtn: byRole('button', {
      name: /issue.changelog.found_on_x_show_more/,
    }),
    changelogRow: (key: string, oldValue: string, newValue: string) =>
      byRole('row', {
        name: new RegExp(
          `issue\\.changelog\\.changed_to\\.issue\\.changelog\\.field\\.${key}\\.${newValue} \\(issue\\.changelog\\.was\\.${oldValue}\\)`,
        ),
      }),

    // Similar issues
    toggleSimilarIssuesBtn: byRole('button', { name: 'issue.filter_similar_issues' }),
    similarIssueTypeLink: byRole('button', { name: 'issue.type.BUG' }),
    similarIssueSeverityLink: byRole('button', { name: 'severity.MAJOR' }),
    similarIssueStatusLink: byRole('button', { name: 'issue.status.OPEN' }),
    similarIssueResolutionLink: byRole('button', { name: 'unresolved' }),
    similarIssueAssigneeLink: byRole('button', { name: 'unassigned' }),
    similarIssueRuleLink: byRole('button', { name: 'Rule Foo' }),
    similarIssueTagLink: (name: string) => byRole('button', { name }),
    similarIssueProjectLink: byRole('button', { name: 'qualifier.TRK Project Bar' }),
    similarIssueFileLink: byRole('button', { name: 'qualifier.FIL main.js' }),

    // Comment
    commentsList: () => {
      const list = byRole('list')
        .getAll()
        .find((el) => el.getAttribute('data-testid') === 'issue-comments');
      if (list === undefined) {
        throw new Error('Could not find comments list');
      }
      return list;
    },
    commentAddBtn: byRole('button', { name: 'issue.comment.add_comment' }),
    commentEditBtn: byRole('button', { name: 'issue.comment.edit' }),
    commentTextInput: byRole('textbox', { name: 'issue.comment.enter_comment' }),
    commentSaveBtn: byRole('button', { name: 'issue.comment.formlink' }),
    commentUpdateBtn: byRole('button', { name: 'save' }),
    commentDeleteBtn: byRole('button', { name: 'issue.comment.delete' }),
    commentConfirmDeleteBtn: byRole('button', { name: 'delete' }),

    // Type
    updateTypeBtn: (currentType: IssueType) =>
      byLabelText(`issue.type.type_x_click_to_change.issue.type.${currentType}`),
    setTypeBtn: (type: IssueType) => byText(`issue.type.${type}`),

    // Severity
    updateSeverityBtn: (currentSeverity: IssueSeverity) =>
      byLabelText(`issue.severity.severity_x_click_to_change.severity.${currentSeverity}`),
    setSeverityBtn: (severity: IssueSeverity) => byText(`severity.${severity}`),

    // Status
    updateStatusBtn: (currentStatus: IssueStatus) =>
      byLabelText(`issue.transition.status_x_click_to_change.issue.issue_status.${currentStatus}`),
    setStatusBtn: (transition: IssueTransition) => byText(`issue.transition.${transition}`),

    // Assignee
    assigneeSearchInput: byLabelText('search.search_for_users'),
    updateAssigneeBtn: (currentAssignee: string) =>
      byRole('combobox', {
        name: `issue.assign.assigned_to_x_click_to_change.${currentAssignee}`,
      }),
    setAssigneeBtn: (name: RegExp) => byLabelText(name),

    // Tags
    tagsSearchInput: byRole('searchbox'),
    updateTagsBtn: (currentTags?: string[]) =>
      byRole('button', { name: `${currentTags ? currentTags.join(' ') : 'issue.no_tag'} +` }),
    toggleTagCheckbox: (name: string) => byRole('checkbox', { name }),
  };

  const ui = {
    ...selectors,
    async addComment(content: string) {
      await user.click(selectors.commentAddBtn.get());
      await user.type(selectors.commentTextInput.get(), content);
      await user.click(selectors.commentSaveBtn.get());
    },
    async updateComment(content: string) {
      await user.click(selectors.commentEditBtn.get());
      await user.type(selectors.commentTextInput.get(), content);
      await user.keyboard(`{Control>}{${KeyboardKeys.Enter}}{/Control}`);
    },
    async deleteComment() {
      await user.click(selectors.commentDeleteBtn.get());
      await user.click(selectors.commentConfirmDeleteBtn.get());
    },
    async updateType(currentType: IssueType, newType: IssueType) {
      await user.click(selectors.updateTypeBtn(currentType).get());
      await user.click(selectors.setTypeBtn(newType).get());
    },
    async updateSeverity(currentSeverity: IssueSeverity, newSeverity: IssueSeverity) {
      await user.click(selectors.updateSeverityBtn(currentSeverity).get());
      await user.click(selectors.setSeverityBtn(newSeverity).get());
    },
    async updateStatus(currentStatus: IssueStatus, transition: IssueTransition) {
      await user.click(selectors.updateStatusBtn(currentStatus).get());
      await user.click(selectors.setStatusBtn(transition).get());
    },
    async updateAssignee(currentAssignee: string, newAssignee: string) {
      await user.click(selectors.updateAssigneeBtn(currentAssignee).get());
      await user.type(selectors.assigneeSearchInput.get(), newAssignee);
      await user.click(selectors.setAssigneeBtn(new RegExp(newAssignee)).get());
    },
    async addTag(tag: string, currentTagList?: string[]) {
      await user.click(selectors.updateTagsBtn(currentTagList).get());
      await user.click(selectors.toggleTagCheckbox(tag).get());
      await user.keyboard('{Escape}');
    },
    async showChangelog() {
      await user.click(selectors.toggleChangelogBtn.get());
    },

    async toggleCheckbox() {
      await user.click(selectors.checkbox.get());
    },
    async clickIssueMessage() {
      await user.click(selectors.issueMessageLink.get());
    },
    async pressDismissShortcut() {
      await user.keyboard(`{${KeyboardKeys.Escape}}`);
    },
    async pressTransitionShortcut() {
      await user.keyboard(`{${KeyboardKeys.KeyF}}`);
    },
    async pressAssignShortcut() {
      await user.keyboard(`{${KeyboardKeys.KeyA}}`);
    },
    async pressAssignToMeShortcut() {
      await user.keyboard(`{${KeyboardKeys.KeyM}}`);
    },
    async pressSeverityShortcut() {
      await user.keyboard(`{${KeyboardKeys.KeyI}}`);
    },
    async pressTagsShortcut() {
      await user.keyboard(`{${KeyboardKeys.KeyT}}`);
    },
    async pressCheckShortcut() {
      await user.keyboard(`{${KeyboardKeys.Space}}`);
    },
  };

  return { ui, user };
}

function renderIssue(
  props: Partial<Omit<ComponentPropsType<typeof Issue>, 'onChange' | 'onPopupToggle'>> = {},
) {
  function Wrapper(
    wrapperProps: Omit<ComponentPropsType<typeof Issue>, 'onChange' | 'onPopupToggle'>,
  ) {
    const [issue, setIssue] = React.useState(wrapperProps.issue);
    const [openPopup, setOpenPopup] = React.useState<string | undefined>();
    return (
      <Issue
        issue={issue}
        openPopup={openPopup}
        onChange={(newIssue) => {
          setIssue({ ...issue, ...newIssue });
        }}
        onPopupToggle={(_key, popup, open) => {
          setOpenPopup(open === false ? undefined : popup);
        }}
        {...omit(wrapperProps, 'issue')}
      />
    );
  }

  return renderAppRoutes(
    'issues?scopes=MAIN&impactSeverities=LOW&types=VULNERABILITY',
    () => (
      <Route
        path="issues"
        element={<Wrapper onSelect={jest.fn()} issue={mockIssue()} selected={false} {...props} />}
      />
    ),
    {
      currentUser: mockLoggedInUser({ login: 'leia', name: 'Organa' }),
    },
  );
}
