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

import { render, screen } from '@testing-library/react';
import { mockBranch, mockMainBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import BranchLikeIcon, { BranchLikeIconProps } from '../../icon-mappers/BranchLikeIcon';

it('should render the branch icon correctly', () => {
  renderBranchLikeIcon({ branchLike: mockBranch() });
  expect(screen.getByTestId('branch-like-icon-branch')).toBeInTheDocument();
});

it('should render the main branch icon correctly', () => {
  renderBranchLikeIcon({ branchLike: mockMainBranch() });
  expect(screen.getByTestId('branch-like-icon-main-branch')).toBeInTheDocument();
});

it('should render the pull request icon correctly', () => {
  renderBranchLikeIcon({ branchLike: mockPullRequest() });
  expect(screen.getByTestId('branch-like-icon-pull-request')).toBeInTheDocument();
});

function renderBranchLikeIcon(props: BranchLikeIconProps) {
  return render(<BranchLikeIcon {...props} />);
}
