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

import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import { Outlet, Route } from 'react-router-dom';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { getMyProjects, getScannableProjects } from '../../../api/components';
import NotificationsMock from '../../../api/mocks/NotificationsMock';
import UserTokensMock from '../../../api/mocks/UserTokensMock';
import { mockUserToken } from '../../../helpers/mocks/token';
import { setKeyboardShortcutEnabled } from '../../../helpers/preferences';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { NotificationGlobalType, NotificationProjectType } from '../../../types/notifications';
import { Permissions } from '../../../types/permissions';
import { TokenType } from '../../../types/token';
import { CurrentUser } from '../../../types/users';
import routes from '../routes';

jest.mock('../../../helpers/preferences', () => ({
  getKeyboardShortcutEnabled: jest.fn().mockResolvedValue(true),
  setKeyboardShortcutEnabled: jest.fn(),
}));

jest.mock('../../../helpers/dates', () => {
  return {
    ...jest.requireActual('../../../helpers/dates'),
    now: jest.fn(() => new Date('2022-06-01T12:00:00Z')),
  };
});

jest.mock('../../../api/settings', () => {
  const { SettingsKey } = jest.requireActual('../../../types/settings');
  return {
    ...jest.requireActual('../../../api/settings'),
    getAllValues: jest.fn().mockResolvedValue([
      {
        key: SettingsKey.TokenMaxAllowedLifetime,
        value: 'No expiration',
      },
    ]),
  };
});

jest.mock('../../../api/components', () => ({
  getMyProjects: jest.fn().mockResolvedValue({
    paging: { total: 2, pageIndex: 1, pageSize: 10 },
    projects: [
      {
        key: 'proj1',
        name: 'Project 1',
        links: [
          {
            type: 'homepage',
            href: 'http://www.sonarsource.com/proj1',
          },
          {
            type: 'issue',
            href: 'http://jira.sonarsource.com/',
          },
          {
            type: 'scm',
            href: 'https://github.com/SonarSource/project1',
          },
          {
            type: 'ci',
            href: 'https://travis.com/project1',
          },
          {
            type: 'other',
            href: 'https://apidocs.project1.net',
          },
        ],
        description: 'project description',
        lastAnalysisDate: '2019-01-04T09:51:48+0000',
        qualityGate: 'OK',
      },
      {
        key: 'proj2',
        name: 'Project 2',
        links: [],
      },
    ],
  }),
  getSuggestions: jest.fn().mockResolvedValue({
    results: [
      {
        q: 'TRK',
        items: [
          {
            isFavorite: true,
            isRecentlyBrowsed: true,
            key: 'sonarqube',
            match: 'SonarQube',
            name: 'SonarQube',
            project: '',
          },
          {
            isFavorite: false,
            isRecentlyBrowsed: false,
            key: 'sonarcloud',
            match: 'Sonarcloud',
            name: 'Sonarcloud',
            project: '',
          },
        ],
      },
    ],
  }),
  getScannableProjects: jest.fn().mockResolvedValue({
    projects: [
      {
        key: 'project-key-1',
        name: 'Project Name 1',
      },
      {
        key: 'project-key-2',
        name: 'Project Name 2',
      },
    ],
  }),
}));

jest.mock('../../../api/users', () => ({
  getIdentityProviders: jest.fn().mockResolvedValue({
    identityProviders: [
      {
        key: 'github',
        name: 'GitHub',
        iconPath: '/images/alm/github.svg',
        backgroundColor: '#444444',
      },
    ],
  }),
  changePassword: jest.fn().mockResolvedValue(true),
}));

it('should handle a currentUser not logged in', () => {
  const replace = jest.fn();
  const locationMock = jest.spyOn(window, 'location', 'get').mockReturnValue({
    pathname: '/account',
    search: '',
    hash: '',
    replace,
  } as unknown as Location);

  renderAccountApp(mockCurrentUser());

  // Make sure we're redirected to the login screen
  expect(replace).toHaveBeenCalledWith('/sessions/new?return_to=%2Faccount');

  locationMock.mockRestore();
});

it('should render the top menu', async () => {
  const name = 'Tyler Durden';
  renderAccountApp(mockLoggedInUser({ name }));

  expect(await screen.findByText(name)).toBeInTheDocument();

  const topMenuNavigationItems = [
    'my_account.profile',
    'my_account.security',
    'my_account.notifications',
    'my_account.projects',
  ];
  topMenuNavigationItems.forEach((itemName) => {
    expect(byRole('navigation').byRole('link', { name: itemName }).get()).toBeInTheDocument();
  });
});

describe('profile page', () => {
  it('should display all the information', async () => {
    const loggedInUser = mockLoggedInUser({
      email: 'email@company.com',
      groups: ['group1'],
      scmAccounts: ['account1'],
    });
    renderAccountApp(loggedInUser);

    expect(await screen.findByText('group1')).toBeInTheDocument();
    expect(screen.getByText('account1')).toBeInTheDocument();
    expect(screen.getAllByText(loggedInUser.login)).toHaveLength(2);
    expect(screen.getAllByText(loggedInUser.email!)).toHaveLength(2);
  });

  it('should handle missing info', () => {
    const loggedInUser = mockLoggedInUser({ local: true, login: '' });
    renderAccountApp(loggedInUser);

    expect(screen.queryByText('my_profile.login')).not.toBeInTheDocument();
    expect(screen.queryByText('my_profile.email')).not.toBeInTheDocument();
    expect(screen.queryByText('my_profile.groups')).not.toBeInTheDocument();
    expect(screen.queryByText('my_profile.scm_accounts')).not.toBeInTheDocument();
  });

  it('should handle known external Providers', async () => {
    const loggedInUser = mockLoggedInUser({
      externalProvider: 'github',
      externalIdentity: 'gh_id',
    });
    renderAccountApp(loggedInUser);

    expect(await screen.findByAltText('GitHub')).toBeInTheDocument();
  });

  it('should handle unknown external Providers', async () => {
    const loggedInUser = mockLoggedInUser({
      externalProvider: 'swag',
      externalIdentity: 'king_of_swag',
    });
    renderAccountApp(loggedInUser);

    expect(
      await screen.findByText(`${loggedInUser.externalProvider}: ${loggedInUser.externalIdentity}`),
    ).toBeInTheDocument();
  });

  it('should allow toggling keyboard shortcuts', async () => {
    const user = userEvent.setup();
    renderAccountApp(mockLoggedInUser());

    const toggle = screen.getByRole('switch');
    expect(toggle).toBeInTheDocument();

    await user.click(toggle);

    expect(setKeyboardShortcutEnabled).toHaveBeenCalledWith(false);
  });
});

describe('security page', () => {
  let tokenMock: UserTokensMock;

  beforeAll(() => {
    tokenMock = new UserTokensMock();
  });

  afterEach(() => {
    tokenMock.reset();
  });

  const securityPagePath = 'account/security';

  it.each([
    ['user', TokenType.User],
    ['global', TokenType.Global],
    ['project analysis', TokenType.Project],
  ])(
    'should allow %s token creation/revocation and display existing tokens',
    async (_, tokenTypeOption) => {
      const user = userEvent.setup();

      renderAccountApp(
        mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
        securityPagePath,
      );

      expect(await screen.findByText('users.tokens.generate')).toBeInTheDocument();
      await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(3)); // 2 tokens + header

      // Add the token
      const newTokenName = 'importantToken';
      const input = screen.getByPlaceholderText('users.tokens.enter_name');
      const generateButton = screen.getByRole('button', { name: 'users.generate' });
      expect(input).toBeInTheDocument();
      await user.click(input);
      await user.keyboard(newTokenName);

      expect(generateButton).toBeDisabled();

      const tokenTypeLabel = `users.tokens.${tokenTypeOption}`;
      const tokenTypeShortLabel = `users.tokens.${tokenTypeOption}.short`;

      // eslint-disable-next-line jest/no-conditional-in-test
      if (tokenTypeOption === TokenType.Project) {
        await user.click(ui.tokenTypeSelect.get());
        await user.click(byRole('option', { name: tokenTypeLabel }).get());

        // eslint-disable-next-line jest/no-conditional-expect
        expect(generateButton).toBeDisabled();
        // eslint-disable-next-line jest/no-conditional-expect
        expect(screen.getByRole('textbox', { name: 'users.tokens.name' })).toBeInTheDocument();
        // eslint-disable-next-line jest/no-conditional-expect
        expect(screen.getAllByRole('combobox')).toHaveLength(3);

        await user.click(ui.projectSelect.get());
        await user.click(byRole('option', { name: 'Project Name 1' }).get());
        // eslint-disable-next-line jest/no-conditional-expect
        expect(generateButton).toBeEnabled();
      } else {
        await user.click(ui.tokenTypeSelect.get());
        await user.click(byRole('option', { name: tokenTypeLabel }).get());
        // eslint-disable-next-line jest/no-conditional-expect
        expect(generateButton).toBeEnabled();
      }

      await user.click(generateButton);

      expect(
        await screen.findByText(`users.tokens.new_token_created.${newTokenName}`),
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Copy to clipboard' })).toBeInTheDocument();

      const lastTokenCreated = tokenMock.getLastToken();
      // eslint-disable-next-line jest/no-conditional-in-test
      if (lastTokenCreated === undefined) {
        throw new Error("Couldn't find the latest generated token.");
      }
      // eslint-disable-next-line jest/no-conditional-in-test
      expect(screen.getByLabelText('users.new_token')).toHaveTextContent(
        // eslint-disable-next-line jest/no-conditional-in-test
        lastTokenCreated.token ?? '',
      );

      expect(screen.getAllByRole('row')).toHaveLength(4); // 3 tokens + header

      const row = screen.getByRole('row', {
        name: new RegExp(`^${newTokenName}`),
      });

      expect(await within(row).findByText(tokenTypeShortLabel)).toBeInTheDocument();
      // eslint-disable-next-line jest/no-conditional-in-test
      if (tokenTypeOption === TokenType.Project) {
        // eslint-disable-next-line jest/no-conditional-expect
        expect(await within(row).findByText('Project Name 1')).toBeInTheDocument();
      }

      // Revoke the token
      const revokeButtons = within(row).getByRole('button', {
        name: 'users.tokens.revoke_label.importantToken',
      });
      await user.click(revokeButtons);

      expect(
        screen.getByRole('heading', { name: 'users.tokens.revoke_label.importantToken' }),
      ).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'yes' }));

      await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(3)); // 2 tokens + header
    },
  );

  it('should flag expired tokens as such', async () => {
    tokenMock.tokens.push(
      mockUserToken({
        name: 'expired token',
        isExpired: true,
        expirationDate: '2021-01-23T19:25:19+0000',
      }),
    );

    renderAccountApp(
      mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
      securityPagePath,
    );

    expect(await screen.findByText('users.tokens.generate')).toBeInTheDocument();

    // expired token is flagged as such
    const expiredTokenRow = await screen.findByRole('row', { name: /expired token/ });
    expect(within(expiredTokenRow).getByText('my_account.tokens.expired')).toBeInTheDocument();

    // unexpired token is not flagged
    const unexpiredTokenRow = screen.getAllByRole('row')[0];
    expect(
      within(unexpiredTokenRow).queryByText('my_account.tokens.expired'),
    ).not.toBeInTheDocument();
  });

  it("should not suggest creating a Project token if the user doesn't have at least one scannable Projects", async () => {
    const user = userEvent.setup();
    jest.mocked(getScannableProjects).mockResolvedValueOnce({
      projects: [],
    });
    renderAccountApp(
      mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
      securityPagePath,
    );

    expect(await screen.findByText('users.tokens.generate')).toBeInTheDocument();

    await user.click(ui.tokenTypeSelect.get());
    expect(screen.queryByText(`users.tokens.${TokenType.Project}`)).not.toBeInTheDocument();
  });

  it('should preselect the user token type if the user has no scan rights', async () => {
    jest.mocked(getScannableProjects).mockResolvedValueOnce({
      projects: [],
    });
    renderAccountApp(mockLoggedInUser(), securityPagePath);

    const globalToken = await screen.findByText(`users.tokens.${TokenType.User}`);
    expect(globalToken).toBeInTheDocument();
  });

  it('should preselect the only project the user has access to if they select project token', async () => {
    const user = userEvent.setup();
    jest.mocked(getScannableProjects).mockResolvedValueOnce({
      projects: [
        {
          key: 'project-key-1',
          name: 'Project Name 1',
        },
      ],
    });
    renderAccountApp(
      mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
      securityPagePath,
    );
    expect(await screen.findByText('users.tokens.generate')).toBeInTheDocument();

    await user.click(ui.tokenTypeSelect.get());
    await user.click(byRole('option', { name: `users.tokens.${TokenType.Project}` }).get());

    expect(screen.getByText('Project Name 1')).toBeInTheDocument();
  });

  it('should allow local users to change password', async () => {
    const user = userEvent.setup();
    renderAccountApp(mockLoggedInUser({ local: true }), securityPagePath);

    expect(
      await screen.findByRole('heading', { name: 'my_profile.password.title' }),
    ).toBeInTheDocument();

    const oldPasswordField = screen.getByLabelText('my_profile.password.old', {
      selector: 'input',
      exact: false,
    });

    const newPasswordField = screen.getByLabelText(/^password/);
    const confirmPasswordField = screen.getByLabelText(/confirm_password*/i);

    await fillTextField(user, oldPasswordField, '123456old');
    await fillTextField(user, newPasswordField, 'P@ssword12345');
    await fillTextField(user, confirmPasswordField, 'P@ssword12345typo');

    expect(screen.getByText('user.password.do_not_match')).toBeInTheDocument();

    // Backspace to erase the previous content
    // [Backspace>7/] == hold, trigger 7 times and release
    await fillTextField(user, confirmPasswordField, '[Backspace>4/]');

    await user.click(screen.getByRole('button', { name: 'update_verb' }));

    expect(await screen.findByText('my_profile.password.changed')).toBeInTheDocument();
  });
});

describe('notifications page', () => {
  const projectUI = {
    title: byRole('button', { name: 'my_profile.per_project_notifications.add' }),
    addButton: byRole('button', { name: 'my_profile.per_project_notifications.add' }),
    addModalButton: byRole('button', { name: 'add_verb' }),
    searchInput: byRole('searchbox', { name: 'search.placeholder' }),
    sonarQubeProject: byRole('link', { name: 'SonarQube' }),
    checkbox: (type: NotificationProjectType) =>
      byRole('checkbox', {
        name: `notification.dispatcher.description_x.notification.dispatcher.${type}.project`,
      }),
  };

  const globalUI = {
    title: byRole('heading', { name: 'my_profile.overall_notifications.title' }),
    noNotificationForProject: byText('my_account.no_project_notifications'),
    checkbox: (type: NotificationGlobalType) =>
      byRole('checkbox', {
        name: `notification.dispatcher.description_x.notification.dispatcher.${type}`,
      }),
  };

  let notificationsMock: NotificationsMock;
  beforeAll(() => {
    notificationsMock = new NotificationsMock();
  });

  afterEach(() => {
    notificationsMock.reset();
  });

  const notificationsPagePath = 'account/notifications';

  it('should display global notifications status and allow edits', async () => {
    const user = userEvent.setup({ delay: null });

    renderAccountApp(mockLoggedInUser(), notificationsPagePath);

    expect(await globalUI.title.find()).toBeInTheDocument();

    /*
     * Verify Checkbox statuses
     */
    expect(globalUI.checkbox(NotificationGlobalType.ChangesOnMyIssue).get()).toBeChecked();
    expect(globalUI.checkbox(NotificationGlobalType.CeReportTaskFailure).get()).not.toBeChecked();
    expect(globalUI.checkbox(NotificationGlobalType.NewAlerts).get()).not.toBeChecked();
    expect(globalUI.checkbox(NotificationGlobalType.MyNewIssues).get()).not.toBeChecked();
    expect(
      globalUI.checkbox(NotificationGlobalType.QualityGateConditionsMismatch).get(),
    ).not.toBeChecked();

    /*
     * Update notifications
     */
    await user.click(globalUI.checkbox(NotificationGlobalType.ChangesOnMyIssue).get());
    expect(globalUI.checkbox(NotificationGlobalType.ChangesOnMyIssue).get()).not.toBeChecked();

    await user.click(globalUI.checkbox(NotificationGlobalType.NewAlerts).get());
    expect(globalUI.checkbox(NotificationGlobalType.NewAlerts).get()).toBeChecked();
  });

  it('should allow adding notifications for a project', async () => {
    const user = userEvent.setup();

    renderAccountApp(mockLoggedInUser(), notificationsPagePath);

    await user.click(await projectUI.addButton.find());
    expect(projectUI.addModalButton.get()).toBeDisabled();

    await user.keyboard('sonar');
    // navigate within the two results, choose the first:
    await user.keyboard('[ArrowDown][ArrowDown][ArrowUp][Enter]');
    await user.click(projectUI.addModalButton.get());

    expect(projectUI.sonarQubeProject.get()).toBeInTheDocument();
    expect(
      projectUI.checkbox(NotificationProjectType.NewFalsePositiveIssue).get(),
    ).toBeInTheDocument();

    await user.click(projectUI.checkbox(NotificationProjectType.NewAlerts).get());
    expect(projectUI.checkbox(NotificationProjectType.NewAlerts).get()).toBeChecked();

    await user.click(projectUI.checkbox(NotificationProjectType.NewAlerts).get());
    expect(projectUI.checkbox(NotificationProjectType.NewAlerts).get()).not.toBeChecked();
  });

  it('should allow searching for projects', async () => {
    const user = userEvent.setup();

    renderAccountApp(mockLoggedInUser(), notificationsPagePath);

    await user.click(await projectUI.addButton.find());

    expect(screen.getByLabelText('my_account.set_notifications_for.title')).toBeInTheDocument();

    await user.keyboard('sonarqube');

    await user.click(screen.getByText('SonarQube'));

    await user.click(screen.getByRole('button', { name: 'add_verb' }));

    /*
     * search for projects
     */
    await user.click(screen.getByRole('searchbox'));
    await user.keyboard('bla');

    expect(projectUI.sonarQubeProject.query()).not.toBeInTheDocument();

    await user.keyboard('[Backspace>3/]');

    expect(await projectUI.sonarQubeProject.find()).toBeInTheDocument();
  });
});

describe('projects page', () => {
  const projectsPagePath = 'account/projects';

  it('should display the list of projects', async () => {
    renderAccountApp(mockLoggedInUser(), projectsPagePath);

    expect(await screen.findByText('my_account.projects.description')).toBeInTheDocument();

    const project1 = getProjectBlock('Project 1');
    expect(within(project1).getAllByRole('link')).toHaveLength(6);

    const project2 = getProjectBlock('Project 2');

    // FP
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(within(project2).getAllByRole('link')).toHaveLength(1);
  });

  it('should handle no projects', async () => {
    (getMyProjects as jest.Mock).mockResolvedValueOnce({
      paging: { total: 0, pageIndex: 1, pageSize: 10 },
      projects: [],
    });
    renderAccountApp(mockLoggedInUser(), projectsPagePath);

    expect(await screen.findByText('my_account.projects.no_results')).toBeInTheDocument();
  });

  it('should handle pagination', async () => {
    const user = userEvent.setup();

    (getMyProjects as jest.Mock)
      .mockResolvedValueOnce({
        paging: { total: 2, pageIndex: 1, pageSize: 1 },
        projects: [
          {
            key: 'proj1',
            name: 'Project 1',
            links: [],
          },
        ],
      })
      .mockResolvedValueOnce({
        paging: { total: 2, pageIndex: 2, pageSize: 1 },
        projects: [
          {
            key: 'proj2',
            name: 'Project 2',
            links: [],
          },
        ],
      });

    renderAccountApp(mockLoggedInUser(), projectsPagePath);

    // FP
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(await screen.findAllByRole('heading', { name: /Project \d/ })).toHaveLength(1);

    const showMoreButton = await screen.findByRole('button', { name: 'show_more' });
    expect(showMoreButton).toBeInTheDocument();
    await user.click(showMoreButton);

    expect(await screen.findAllByRole('heading', { name: /Project \d/ })).toHaveLength(2);
  });
});

async function fillTextField(user: UserEvent, field: HTMLElement, value: string) {
  await user.click(field);
  await user.keyboard(value);
}

function getProjectBlock(projectName: string) {
  const result = screen
    .getAllByRole('listitem')
    .find((element) => within(element).queryByRole('heading', { name: projectName }) !== null);

  if (!result) {
    // eslint-disable-next-line testing-library/no-debugging-utils
    screen.debug(screen.getAllByRole('listitem'));
    throw new Error(`Could not find project ${projectName}`);
  }

  return result;
}

function renderAccountApp(currentUser: CurrentUser, navigateTo?: string) {
  renderAppRoutes(
    'account',
    () => (
      <Route
        path="/"
        element={
          <>
            <div id="component-nav-portal" />

            <Outlet />
          </>
        }
      >
        {routes()}
      </Route>
    ),
    { currentUser, navigateTo },
  );
}

const ui = {
  tokenTypeSelect: byRole('combobox', { name: 'users.tokens.type' }),
  projectSelect: byRole('combobox', { name: 'users.tokens.project' }),
};
