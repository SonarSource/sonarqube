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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import AlmSettingsServiceMock from '../../../../../api/mocks/AlmSettingsServiceMock';
import BranchesServiceMock from '../../../../../api/mocks/BranchesServiceMock';
import { mockMainBranch, mockPullRequest } from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import { Feature } from '../../../../../types/features';
import { Header, HeaderProps } from '../Header';

jest.mock('../../../../../api/favorites', () => ({
  addFavorite: jest.fn().mockResolvedValue({}),
  removeFavorite: jest.fn().mockResolvedValue({}),
}));

const handler = new BranchesServiceMock();
const almHandler = new AlmSettingsServiceMock();

beforeEach(() => {
  handler.reset();
  almHandler.reset();
});

it('should render correctly when there is only 1 branch', async () => {
  handler.emptyBranchesAndPullRequest();
  handler.addBranch(mockMainBranch({ status: { qualityGateStatus: 'OK' } }));
  renderHeader();
  expect(await screen.findByLabelText('help-tooltip')).toBeInTheDocument();
  expect(screen.getByText('project')).toBeInTheDocument();
  expect(
    await screen.findByRole('button', { name: 'master overview.quality_gate_x.metric.level.OK' }),
  ).toBeDisabled();
});

it('should render correctly when there are multiple branch', async () => {
  const user = userEvent.setup();
  renderHeader();

  expect(
    await screen.findByRole('button', { name: 'main overview.quality_gate_x.metric.level.OK' }),
  ).toBeEnabled();

  expect(screen.queryByLabelText('help-tooltip')).not.toBeInTheDocument();

  await user.click(
    screen.getByRole('button', { name: 'main overview.quality_gate_x.metric.level.OK' }),
  );
  expect(screen.getByText('branches.main_branch')).toBeInTheDocument();
  expect(
    screen.getByRole('menuitem', {
      name: '03 – TEST-193 dumb commit overview.quality_gate_x.metric.level.ERROR ERROR',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('menuitem', {
      name: '01 – TEST-191 update master overview.quality_gate_x.metric.level.OK OK',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('menuitem', {
      name: 'normal-branch overview.quality_gate_x.metric.level.ERROR ERROR',
    }),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('menuitem', {
      name: 'normal-branch overview.quality_gate_x.metric.level.ERROR ERROR',
    }),
  );
  expect(screen.getByText('/dashboard?branch=normal-branch&id=header-project')).toBeInTheDocument();
});

it('should show manage branch and pull request button for admin', async () => {
  const user = userEvent.setup();
  renderHeader({
    currentUser: mockLoggedInUser(),
    component: mockComponent({
      key: 'header-project',
      configuration: { showSettings: true },
      breadcrumbs: [{ name: 'project', key: 'project', qualifier: ComponentQualifier.Project }],
    }),
  });
  await user.click(
    await screen.findByRole('button', { name: 'main overview.quality_gate_x.metric.level.OK' }),
  );

  expect(screen.getByRole('link', { name: 'branch_like_navigation.manage' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'branch_like_navigation.manage' })).toHaveAttribute(
    'href',
    '/project/branches?id=header-project',
  );
});

it('should render favorite button if the user is logged in', async () => {
  const user = userEvent.setup();
  renderHeader({ currentUser: mockLoggedInUser() });
  expect(screen.getByRole('button', { name: 'favorite.action.TRK.add' })).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'favorite.action.TRK.add' }));
  expect(
    await screen.findByRole('button', { name: 'favorite.action.TRK.remove' }),
  ).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'favorite.action.TRK.remove' }));
  expect(screen.getByRole('button', { name: 'favorite.action.TRK.add' })).toBeInTheDocument();
});

it.each([['github'], ['gitlab'], ['bitbucket'], ['azure']])(
  'should show correct %s links for a PR',
  async (alm: string) => {
    handler.emptyBranchesAndPullRequest();
    handler.addPullRequest(mockPullRequest({ url: alm }));
    renderHeader(
      {
        currentUser: mockLoggedInUser(),
      },
      undefined,
      'pullRequest=1001&id=compa',
    );
    const image = await screen.findByAltText(alm);
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute('src', `/images/alm/${alm}.svg`);
  },
);

it('should show the correct help tooltip for applications', async () => {
  handler.emptyBranchesAndPullRequest();
  handler.addBranch(mockMainBranch());
  renderHeader({
    currentUser: mockLoggedInUser(),
    component: mockComponent({
      key: 'header-project',
      configuration: { showSettings: true },
      breadcrumbs: [{ name: 'project', key: 'project', qualifier: ComponentQualifier.Application }],
      qualifier: 'APP',
    }),
  });
  expect(await screen.findByText('application.branches.help')).toBeInTheDocument();
  expect(screen.getByText('application.branches.link')).toBeInTheDocument();
});

it('should show the correct help tooltip when branch support is not enabled', async () => {
  handler.emptyBranchesAndPullRequest();
  handler.addBranch(mockMainBranch());
  almHandler.handleSetProjectBinding(AlmKeys.GitLab, {
    almSetting: 'key',
    project: 'header-project',
    repository: 'header-project',
    monorepo: true,
  });
  renderHeader(
    {
      currentUser: mockLoggedInUser(),
    },
    [],
  );
  expect(
    await screen.findByText('branch_like_navigation.no_branch_support.title.mr'),
  ).toBeInTheDocument();
  expect(
    screen.getByText('branch_like_navigation.no_branch_support.content_x.mr.alm.gitlab'),
  ).toBeInTheDocument();
});

function renderHeader(
  props?: Partial<HeaderProps>,
  featureList = [Feature.BranchSupport],
  params?: string,
) {
  return renderApp(
    '/',
    <Header
      component={mockComponent({
        key: 'header-project',
        breadcrumbs: [{ name: 'project', key: 'project', qualifier: ComponentQualifier.Project }],
      })}
      currentUser={mockCurrentUser()}
      {...props}
    />,
    { featureList, navigateTo: params ? `/?id=header-project&${params}` : '/?id=header-project' },
  );
}
