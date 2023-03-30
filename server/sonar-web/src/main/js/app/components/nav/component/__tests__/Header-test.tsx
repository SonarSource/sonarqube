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
import { mockProjectAlmBindingResponse } from '../../../../../helpers/mocks/alm-settings';
import {
  mockMainBranch,
  mockPullRequest,
  mockSetOfBranchAndPullRequestForBranchSelector,
} from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import { ComponentQualifier } from '../../../../../types/component';
import { Feature } from '../../../../../types/features';
import { BranchStatusContext } from '../../../branch-status/BranchStatusContext';
import { Header, HeaderProps } from '../Header';

jest.mock('../../../../../api/favorites', () => ({
  addFavorite: jest.fn().mockResolvedValue({}),
  removeFavorite: jest.fn().mockResolvedValue({}),
}));

it('should render correctly when there is only 1 branch', () => {
  renderHeader({ branchLikes: [mockMainBranch()] });
  expect(screen.getByText('project')).toBeInTheDocument();
  expect(screen.getByLabelText('help-tooltip')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'branch-1 overview.quality_gate_x.OK' })
  ).toBeDisabled();
});

it('should render correctly when there are multiple branch', async () => {
  const user = userEvent.setup();
  renderHeader();
  expect(screen.getByRole('button', { name: 'branch-1 overview.quality_gate_x.OK' })).toBeEnabled();
  expect(screen.queryByLabelText('help-tooltip')).not.toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'branch-1 overview.quality_gate_x.OK' }));
  expect(screen.getByText('branches.main_branch')).toBeInTheDocument();
  expect(
    screen.getByRole('menuitem', { name: 'branch-2 overview.quality_gate_x.ERROR ERROR' })
  ).toBeInTheDocument();
  expect(screen.getByRole('menuitem', { name: 'branch-3' })).toBeInTheDocument();
  expect(screen.getByRole('menuitem', { name: '1 – PR-1' })).toBeInTheDocument();
  expect(screen.getByRole('menuitem', { name: '2 – PR-2' })).toBeInTheDocument();

  await user.click(
    screen.getByRole('menuitem', { name: 'branch-2 overview.quality_gate_x.ERROR ERROR' })
  );
  expect(screen.getByText('/dashboard?branch=branch-2&id=my-project')).toBeInTheDocument();
});

it('should show manage branch and pull request button for admin', async () => {
  const user = userEvent.setup();
  renderHeader({
    currentUser: mockLoggedInUser(),
    component: mockComponent({
      configuration: { showSettings: true },
      breadcrumbs: [{ name: 'project', key: 'project', qualifier: ComponentQualifier.Project }],
    }),
  });
  await user.click(screen.getByRole('button', { name: 'branch-1 overview.quality_gate_x.OK' }));

  expect(screen.getByRole('link', { name: 'branch_like_navigation.manage' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'branch_like_navigation.manage' })).toHaveAttribute(
    'href',
    '/project/branches?id=my-project'
  );
});

it('should render favorite button if the user is logged in', async () => {
  const user = userEvent.setup();
  renderHeader({ currentUser: mockLoggedInUser() });
  expect(screen.getByRole('button', { name: 'favorite.action.TRK.add' })).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'favorite.action.TRK.add' }));
  expect(
    await screen.findByRole('button', { name: 'favorite.action.TRK.remove' })
  ).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'favorite.action.TRK.remove' }));
  expect(screen.getByRole('button', { name: 'favorite.action.TRK.add' })).toBeInTheDocument();
});

it.each([['github'], ['gitlab'], ['bitbucket'], ['azure']])(
  'should show correct %s links for a PR',
  (alm: string) => {
    renderHeader({
      currentUser: mockLoggedInUser(),
      currentBranchLike: mockPullRequest({
        key: '1',
        title: 'PR-1',
        status: { qualityGateStatus: 'OK' },
        url: alm,
      }),
      branchLikes: [
        mockPullRequest({
          key: '1',
          title: 'PR-1',
          status: { qualityGateStatus: 'OK' },
          url: alm,
        }),
      ],
    });
    const image = screen.getByAltText(alm);
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute('src', `/images/alm/${alm}.svg`);
  }
);

it('should show the correct help tooltip for applications', () => {
  renderHeader({
    currentUser: mockLoggedInUser(),
    component: mockComponent({
      configuration: { showSettings: true },
      breadcrumbs: [{ name: 'project', key: 'project', qualifier: ComponentQualifier.Application }],
      qualifier: 'APP',
    }),
    branchLikes: [mockMainBranch()],
  });
  expect(screen.getByText('application.branches.help')).toBeInTheDocument();
  expect(screen.getByText('application.branches.link')).toBeInTheDocument();
});

it('should show the correct help tooltip when branch support is not enabled', () => {
  renderHeader(
    {
      currentUser: mockLoggedInUser(),
      projectBinding: mockProjectAlmBindingResponse({
        alm: AlmKeys.GitLab,
        key: 'key',
        monorepo: true,
      }),
    },
    []
  );
  expect(screen.getByText('branch_like_navigation.no_branch_support.title.mr')).toBeInTheDocument();
  expect(
    screen.getByText('branch_like_navigation.no_branch_support.content_x.mr.alm.gitlab')
  ).toBeInTheDocument();
});

function renderHeader(props?: Partial<HeaderProps>, featureList = [Feature.BranchSupport]) {
  const branchLikes = mockSetOfBranchAndPullRequestForBranchSelector();

  return renderApp(
    '/',
    <BranchStatusContext.Provider
      value={{
        branchStatusByComponent: {
          'my-project': {
            'branch-branch-1': {
              status: 'OK',
            },
            'branch-branch-2': {
              status: 'ERROR',
            },
          },
        },
        fetchBranchStatus: () => {
          /*noop*/
        },
        updateBranchStatus: () => {
          /*noop*/
        },
      }}
    >
      <Header
        branchLikes={branchLikes}
        component={mockComponent({
          breadcrumbs: [{ name: 'project', key: 'project', qualifier: ComponentQualifier.Project }],
        })}
        currentBranchLike={branchLikes[0]}
        currentUser={mockCurrentUser()}
        {...props}
      />
    </BranchStatusContext.Provider>,
    { featureList }
  );
}
