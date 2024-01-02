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
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import * as React from 'react';
import { byLabelText, byRole, byText } from 'testing-library-selector';
import { getAlmSettingsNoCatch } from '../../../api/alm-settings';
import { getScannableProjects } from '../../../api/components';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import UserTokensMock from '../../../api/mocks/UserTokensMock';
import {
  mockGithubBindingDefinition,
  mockProjectAlmBindingResponse,
} from '../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../types/alm-settings';
import { Feature } from '../../../types/features';
import { Permissions } from '../../../types/permissions';
import { SettingsKey } from '../../../types/settings';
import { withRouter } from '../../hoc/withRouter';
import { TutorialSelection } from '../TutorialSelection';
import { TutorialModes } from '../types';

jest.mock('../../../api/settings');
jest.mock('../../../api/user-tokens');

jest.mock('../../../helpers/urls', () => ({
  ...jest.requireActual('../../../helpers/urls'),
  getHostUrl: jest.fn().mockReturnValue('http://host.url'),
}));

jest.mock('../../../api/alm-settings', () => ({
  getAlmSettingsNoCatch: jest.fn().mockRejectedValue(null),
}));

jest.mock('../../../api/components', () => ({
  getScannableProjects: jest.fn().mockResolvedValue({ projects: [] }),
}));

let settingsMock: SettingsServiceMock;
let tokenMock: UserTokensMock;

beforeAll(() => {
  settingsMock = new SettingsServiceMock();
  tokenMock = new UserTokensMock();
});

afterEach(() => {
  tokenMock.reset();
  settingsMock.reset();
});

beforeEach(jest.clearAllMocks);

const ui = {
  loading: byLabelText('loading'),
  noScanRights: byText('onboarding.tutorial.no_scan_rights'),
  chooseTutorialBtn: (mode: TutorialModes) =>
    byRole('button', { name: `onboarding.tutorial.choose_method.${mode}` }),
};

it.each([
  [TutorialModes.Jenkins, 'onboarding.tutorial.with.jenkins.title'],
  [TutorialModes.AzurePipelines, 'onboarding.tutorial.with.azure_pipelines.title'],
  [
    TutorialModes.BitbucketPipelines,
    'onboarding.tutorial.with.bitbucket_pipelines.create_secret.title',
  ],
  [TutorialModes.GitHubActions, 'onboarding.tutorial.with.github_action.create_secret.title'],
  [TutorialModes.GitLabCI, 'onboarding.tutorial.with.gitlab_ci.title'],
  [TutorialModes.Local, 'onboarding.project_analysis.header'],
  [TutorialModes.OtherCI, 'onboarding.project_analysis.header'],
])('should behave correctly for %s', async (mode, title) => {
  const user = userEvent.setup();
  renderTutorialSelection();
  await waitOnDataLoaded();

  expect(screen.getByText('onboarding.tutorial.choose_method')).toBeInTheDocument();

  await user.click(ui.chooseTutorialBtn(mode).get());
  expect(screen.getByText(title)).toBeInTheDocument();
});

it.each([
  [
    AlmKeys.GitHub,
    [TutorialModes.GitHubActions, TutorialModes.Jenkins, TutorialModes.AzurePipelines],
  ],
  [AlmKeys.GitLab, [TutorialModes.GitLabCI, TutorialModes.Jenkins]],
  [AlmKeys.Azure, [TutorialModes.AzurePipelines]],
  [AlmKeys.BitbucketServer, [TutorialModes.Jenkins]],
  [AlmKeys.BitbucketCloud, [TutorialModes.BitbucketPipelines, TutorialModes.Jenkins]],
])('should show correct buttons if project is bound to %s', async (alm, modes) => {
  renderTutorialSelection({ projectBinding: mockProjectAlmBindingResponse({ alm }) });
  await waitOnDataLoaded();

  modes.forEach((mode) => expect(ui.chooseTutorialBtn(mode).get()).toBeInTheDocument());
});

it('should correctly fetch the corresponding ALM setting', async () => {
  (getAlmSettingsNoCatch as jest.Mock).mockResolvedValueOnce([
    mockGithubBindingDefinition({ key: 'binding', url: 'https://enterprise.github.com' }),
  ]);
  const user = userEvent.setup();
  renderTutorialSelection({
    projectBinding: mockProjectAlmBindingResponse({ alm: AlmKeys.GitHub, key: 'binding' }),
  });
  await waitOnDataLoaded();

  await startJenkinsTutorial(user);
  expect(screen.getByText('https://enterprise.github.com', { exact: false })).toBeInTheDocument();
});

it('should correctly fetch the instance URL', async () => {
  settingsMock.set(SettingsKey.ServerBaseUrl, 'http://sq.example.com');
  const user = userEvent.setup();
  renderTutorialSelection();
  await waitOnDataLoaded();

  await startLocalTutorial(user);
  expect(
    screen.getByText('-Dsonar.host.url=http://sq.example.com', { exact: false })
  ).toBeInTheDocument();
});

it('should fallback on the host URL', async () => {
  const user = userEvent.setup();
  renderTutorialSelection();
  await waitOnDataLoaded();

  await startLocalTutorial(user);
  expect(
    screen.getByText('-Dsonar.host.url=http://host.url', { exact: false })
  ).toBeInTheDocument();
});

it('should not display a warning if the user has no global scan permission, but can scan the project', async () => {
  (getScannableProjects as jest.Mock).mockResolvedValueOnce({ projects: [{ key: 'foo' }] });
  renderTutorialSelection({ currentUser: mockLoggedInUser() });
  await waitOnDataLoaded();

  expect(ui.noScanRights.query()).not.toBeInTheDocument();
});

it('should correctly display a warning if the user has no scan permissions', async () => {
  renderTutorialSelection({ currentUser: mockLoggedInUser() });
  await waitOnDataLoaded();

  expect(ui.noScanRights.query()).toBeInTheDocument();
});

async function waitOnDataLoaded() {
  await waitFor(() => {
    expect(ui.loading.query()).not.toBeInTheDocument();
  });
}

async function startLocalTutorial(user: UserEvent) {
  await user.click(ui.chooseTutorialBtn(TutorialModes.Local).get());
  await user.click(screen.getByRole('button', { name: 'onboarding.token.generate' }));
  await user.click(screen.getByRole('button', { name: 'continue' }));
  await user.click(screen.getByRole('button', { name: 'onboarding.build.maven' }));
}

async function startJenkinsTutorial(user: UserEvent) {
  await user.click(ui.chooseTutorialBtn(TutorialModes.Jenkins).get());
  await user.click(
    screen.getByRole('button', { name: 'onboarding.tutorial.with.jenkins.prereqs.done' })
  );
}

function renderTutorialSelection(props: Partial<TutorialSelection['props']> = {}) {
  const Wrapper = withRouter(({ router, location, ...subProps }: TutorialSelection['props']) => {
    return <TutorialSelection location={location} router={router} {...subProps} />;
  });

  return renderApp(
    '/',
    <Wrapper
      component={mockComponent({ key: 'foo' })}
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Scan] } })}
      {...props}
    />,
    { featureList: [Feature.BranchSupport] }
  );
}
