/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { UserEvent } from '@testing-library/user-event/dist/types/setup';
import selectEvent from 'react-select-event';
import { getMyProjects, getScannableProjects } from '../../../api/components';
import NotificationsMock from '../../../api/mocks/NotificationsMock';
import UserTokensMock from '../../../api/mocks/UserTokensMock';
import { mockUserToken } from '../../../helpers/mocks/token';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { Permissions } from '../../../types/permissions';
import { TokenType } from '../../../types/token';
import { CurrentUser } from '../../../types/users';
import routes from '../routes';

jest.mock('../../../api/user-tokens');
jest.mock('../../../api/notifications');

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
            href: 'http://www.sonarsource.com/proj1'
          },
          {
            type: 'issue',
            href: 'http://jira.sonarsource.com/'
          },
          {
            type: 'scm',
            href: 'https://github.com/SonarSource/project1'
          },
          {
            type: 'ci',
            href: 'https://travis.com/project1'
          },
          {
            type: 'other',
            href: 'https://apidocs.project1.net'
          }
        ],
        description: 'project description',
        lastAnalysisDate: '2019-01-04T09:51:48+0000',
        qualityGate: 'OK'
      },
      {
        key: 'proj2',
        name: 'Project 2',
        links: []
      }
    ]
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
            project: ''
          },
          {
            isFavorite: false,
            isRecentlyBrowsed: false,
            key: 'sonarcloud',
            match: 'Sonarcloud',
            name: 'Sonarcloud',
            project: ''
          }
        ]
      }
    ]
  }),
  getScannableProjects: jest.fn().mockResolvedValue({
    projects: [
      {
        key: 'project-key-1',
        name: 'Project Name 1'
      },
      {
        key: 'project-key-2',
        name: 'Project Name 2'
      }
    ]
  })
}));

jest.mock('../../../api/users', () => ({
  getIdentityProviders: jest.fn().mockResolvedValue({
    identityProviders: [
      {
        key: 'github',
        name: 'GitHub',
        iconPath: '/images/alm/github-white.svg',
        backgroundColor: '#444444'
      }
    ]
  }),
  changePassword: jest.fn().mockResolvedValue(true)
}));

it('should handle a currentUser not logged in', () => {
  const replace = jest.fn();
  const locationMock = jest.spyOn(window, 'location', 'get').mockReturnValue(({
    pathname: '/account',
    search: '',
    hash: '',
    replace
  } as unknown) as Location);

  renderAccountApp(mockCurrentUser());

  // Make sure we're redirected to the login screen
  expect(replace).toBeCalledWith('/sessions/new?return_to=%2Faccount');

  locationMock.mockRestore();
});

it('should render the top menu', () => {
  const name = 'Tyler Durden';
  renderAccountApp(mockLoggedInUser({ name }));

  expect(screen.getByText(name)).toBeInTheDocument();

  expect(screen.getByText('my_account.profile')).toBeInTheDocument();
  expect(screen.getByText('my_account.security')).toBeInTheDocument();
  expect(screen.getByText('my_account.notifications')).toBeInTheDocument();
  expect(screen.getByText('my_account.projects')).toBeInTheDocument();
});

describe('profile page', () => {
  it('should display all the information', () => {
    const loggedInUser = mockLoggedInUser({
      email: 'email@company.com',
      groups: ['group1'],
      scmAccounts: ['account1']
    });
    renderAccountApp(loggedInUser);

    expect(screen.getAllByText(loggedInUser.login)).toHaveLength(2);
    expect(screen.getAllByText(loggedInUser.email!)).toHaveLength(2);
    expect(screen.getByText('group1')).toBeInTheDocument();
    expect(screen.getByText('account1')).toBeInTheDocument();
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
      externalIdentity: 'gh_id'
    });
    renderAccountApp(loggedInUser);

    expect(await screen.findByAltText('GitHub')).toBeInTheDocument();
  });

  it('should handle unknown external Providers', async () => {
    const loggedInUser = mockLoggedInUser({
      externalProvider: 'swag',
      externalIdentity: 'king_of_swag'
    });
    renderAccountApp(loggedInUser);

    expect(
      await screen.findByText(`${loggedInUser.externalProvider}: ${loggedInUser.externalIdentity}`)
    ).toBeInTheDocument();
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
    ['project analysis', TokenType.Project]
  ])(
    'should allow %s token creation/revocation and display existing tokens',
    async (_, tokenTypeOption) => {
      const user = userEvent.setup();

      renderAccountApp(
        mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
        securityPagePath
      );

      expect(await screen.findByText('users.tokens')).toBeInTheDocument();
      expect(screen.getAllByRole('row')).toHaveLength(3); // 2 tokens + header

      // Add the token
      const newTokenName = 'importantToken';
      const input = screen.getByPlaceholderText('users.enter_token_name');
      const generateButton = screen.getByRole('button', { name: 'users.generate' });
      expect(input).toBeInTheDocument();
      await user.click(input);
      await user.keyboard(newTokenName);

      expect(generateButton).toBeDisabled();

      const tokenTypeLabel = `users.tokens.${tokenTypeOption}`;
      const tokenTypeShortLabel = `users.tokens.${tokenTypeOption}.short`;

      if (tokenTypeOption === TokenType.Project) {
        await selectEvent.select(screen.getAllByRole('textbox')[1], [tokenTypeLabel]);
        expect(generateButton).toBeDisabled();
        expect(screen.getAllByRole('textbox')).toHaveLength(3);
        await selectEvent.select(screen.getAllByRole('textbox')[2], ['Project Name 1']);
        expect(generateButton).not.toBeDisabled();
      } else {
        await selectEvent.select(screen.getAllByRole('textbox')[1], [tokenTypeLabel]);
        expect(generateButton).not.toBeDisabled();
      }

      await user.click(generateButton);

      expect(
        await screen.findByText(`users.tokens.new_token_created.${newTokenName}`)
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'copy' })).toBeInTheDocument();

      const lastTokenCreated = tokenMock.getTokens().pop();
      expect(lastTokenCreated).toBeDefined();
      expect(screen.getByLabelText('users.new_token').textContent).toBe(lastTokenCreated!.token);

      expect(screen.getAllByRole('row')).toHaveLength(4); // 3 tokens + header

      const row = screen.getByRole('row', {
        name: new RegExp(`^${newTokenName}`)
      });

      expect(await within(row).findByText(tokenTypeShortLabel)).toBeInTheDocument();
      if (tokenTypeOption === TokenType.Project) {
        expect(await within(row).findByText('Project Name 1')).toBeInTheDocument();
      }

      // Revoke the token
      const revokeButtons = within(row).getByRole('button', {
        name: 'users.tokens.revoke_token'
      });
      await user.click(revokeButtons);

      expect(
        screen.getByRole('heading', { name: 'users.tokens.revoke_token' })
      ).toBeInTheDocument();

      await user.click(screen.getByText('users.tokens.revoke_token', { selector: 'button' }));

      expect(screen.getAllByRole('row')).toHaveLength(3); // 2 tokens + header
    }
  );

  it('should flag expired tokens as such', async () => {
    tokenMock.tokens.push(
      mockUserToken({
        name: 'expired token',
        isExpired: true,
        expirationDate: '2021-01-23T19:25:19+0000'
      })
    );

    renderAccountApp(
      mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
      securityPagePath
    );

    expect(await screen.findByText('users.tokens')).toBeInTheDocument();

    // expired token is flagged as such
    const expiredTokenRow = screen.getByRole('row', { name: /expired token/ });
    expect(within(expiredTokenRow).getByText('my_account.tokens.expired')).toBeInTheDocument();

    // unexpired token is not flagged
    const unexpiredTokenRow = screen.getAllByRole('row')[0];
    expect(
      within(unexpiredTokenRow).queryByText('my_account.tokens.expired')
    ).not.toBeInTheDocument();
  });

  it("should not suggest creating a Project token if the user doesn't have at least one scannable Projects", async () => {
    (getScannableProjects as jest.Mock).mockResolvedValueOnce({
      projects: []
    });
    renderAccountApp(
      mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
      securityPagePath
    );

    await selectEvent.openMenu(screen.getAllByRole('textbox')[1]);
    expect(screen.queryByText(`users.tokens.${TokenType.Project}`)).not.toBeInTheDocument();
  });

  it('should allow local users to change password', async () => {
    const user = userEvent.setup();
    renderAccountApp(mockLoggedInUser({ local: true }), securityPagePath);

    expect(
      await screen.findByRole('heading', { name: 'my_profile.password.title' })
    ).toBeInTheDocument();

    const oldPasswordField = screen.getByLabelText('my_profile.password.old', {
      selector: 'input',
      exact: false
    });

    const newPasswordField = screen.getByLabelText('my_profile.password.new', {
      selector: 'input',
      exact: false
    });
    const confirmPasswordField = screen.getByLabelText('my_profile.password.confirm', {
      selector: 'input',
      exact: false
    });

    await fillTextField(user, oldPasswordField, '123456old');
    await fillTextField(user, newPasswordField, 'newPassword');
    await fillTextField(user, confirmPasswordField, 'newtypo');

    await user.click(screen.getByRole('button', { name: 'update_verb' }));

    expect(screen.getByText('user.password_doesnt_match_confirmation')).toBeInTheDocument();

    // Backspace to erase the previous content
    // [Backspace>7/] == hold, trigger 7 times and release
    await fillTextField(user, confirmPasswordField, '[Backspace>7/]newPassword');

    await user.click(screen.getByRole('button', { name: 'update_verb' }));

    expect(await screen.findByText('my_profile.password.changed')).toBeInTheDocument();
  });
});

describe('notifications page', () => {
  let notificationsMock: NotificationsMock;
  beforeAll(() => {
    notificationsMock = new NotificationsMock();
  });

  afterEach(() => {
    notificationsMock.reset();
  });

  const notificationsPagePath = 'account/notifications';

  it('should display global notifications status and allow edits', async () => {
    const user = userEvent.setup();

    renderAccountApp(mockLoggedInUser(), notificationsPagePath);

    expect(
      await screen.findByRole('heading', { name: 'my_profile.overall_notifications.title' })
    ).toBeInTheDocument();

    expect(screen.getAllByRole('row')).toHaveLength(5); // 4 + header

    /*
     * Verify Checkbox statuses
     */
    expect(getCheckboxByRowName('notification.dispatcher.ChangesOnMyIssue')).toBeChecked();

    // first row is the header: skip it!
    const otherRows = screen
      .getAllByRole('row', {
        name: (n: string) => n !== 'notification.dispatcher.ChangesOnMyIssue'
      })
      .slice(1);

    otherRows.forEach(row => {
      expect(within(row).getByRole('checkbox')).not.toBeChecked();
    });

    // Make sure the second block is empty
    expect(screen.getByText('my_account.no_project_notifications')).toBeInTheDocument();

    /*
     * Update notifications
     */
    await user.click(getCheckboxByRowName('notification.dispatcher.ChangesOnMyIssue'));
    expect(getCheckboxByRowName('notification.dispatcher.ChangesOnMyIssue')).not.toBeChecked();

    await user.click(getCheckboxByRowName('notification.dispatcher.NewAlerts'));
    expect(getCheckboxByRowName('notification.dispatcher.NewAlerts')).toBeChecked();
  });

  it('should allow adding notifications for a project', async () => {
    const user = userEvent.setup();

    renderAccountApp(mockLoggedInUser(), notificationsPagePath);

    await user.click(
      await screen.findByRole('button', { name: 'my_profile.per_project_notifications.add' })
    );

    expect(await screen.findByLabelText('search_verb', { selector: 'input' })).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'add_verb' })).toBeDisabled();

    await user.keyboard('sonar');
    // navigate within the two results, choose the first:
    await user.keyboard('[ArrowDown][ArrowDown][ArrowUp][Enter]');

    const addButton = screen.getByRole('button', { name: 'add_verb' });
    expect(addButton).not.toBeDisabled();

    await user.click(addButton);

    expect(screen.getByRole('heading', { name: 'SonarQube' })).toBeInTheDocument();
    expect(
      getCheckboxByRowName('notification.dispatcher.NewFalsePositiveIssue.project')
    ).toBeInTheDocument();

    await user.click(getCheckboxByRowName('notification.dispatcher.NewAlerts.project'));
    expect(getCheckboxByRowName('notification.dispatcher.NewAlerts.project')).toBeChecked();

    expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(2);

    await user.click(getCheckboxByRowName('notification.dispatcher.NewAlerts.project'));
    expect(getCheckboxByRowName('notification.dispatcher.NewAlerts.project')).not.toBeChecked();
  });

  it('should allow searching for projects', async () => {
    const user = userEvent.setup();

    renderAccountApp(mockLoggedInUser(), notificationsPagePath);

    await user.click(
      screen.getByRole('button', { name: 'my_profile.per_project_notifications.add' })
    );
    expect(await screen.findByLabelText('search_verb', { selector: 'input' })).toBeInTheDocument();
    await user.keyboard('sonarqube');

    await user.click(screen.getByText('SonarQube'));

    await user.click(screen.getByRole('button', { name: 'add_verb' }));

    /*
     * search for projects
     */
    await user.click(screen.getByRole('searchbox'));
    await user.keyboard('bla');

    await waitFor(() => {
      expect(screen.queryByRole('heading', { name: 'SonarQube' })).not.toBeInTheDocument();
    });

    await user.keyboard('[Backspace>3/]');

    expect(await screen.findByRole('heading', { name: 'SonarQube' })).toBeInTheDocument();
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
    expect(within(project2).getAllByRole('link')).toHaveLength(1);
  });

  it('should handle no projects', async () => {
    (getMyProjects as jest.Mock).mockResolvedValueOnce({
      paging: { total: 0, pageIndex: 1, pageSize: 10 },
      projects: []
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
            links: []
          }
        ]
      })
      .mockResolvedValueOnce({
        paging: { total: 2, pageIndex: 2, pageSize: 1 },
        projects: [
          {
            key: 'proj2',
            name: 'Project 2',
            links: []
          }
        ]
      });

    renderAccountApp(mockLoggedInUser(), projectsPagePath);

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
    .find(element => within(element).queryByRole('heading', { name: projectName }) !== null);

  if (!result) {
    // eslint-disable-next-line testing-library/no-debugging-utils
    screen.debug(screen.getAllByRole('listitem'));
    throw new Error(`Could not find project ${projectName}`);
  }

  return result;
}

function getCheckboxByRowName(name: string) {
  return within(screen.getByRole('row', { name })).getByRole('checkbox');
}

function renderAccountApp(currentUser: CurrentUser, navigateTo?: string) {
  renderApp('account', routes, { currentUser, navigateTo });
}
