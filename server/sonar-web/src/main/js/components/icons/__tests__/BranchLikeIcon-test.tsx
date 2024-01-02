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
import { render } from '@testing-library/react';
import * as React from 'react';
import { mockBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import BranchLikeIcon, { BranchLikeIconProps } from '../BranchLikeIcon';

it('should render branch icon correctly', () => {
  renderBranchLikeIcon({ branchLike: mockBranch() });
  expect(document.body.innerHTML).toMatchSnapshot();
});

it('should render pull request icon correctly', () => {
  renderBranchLikeIcon({ branchLike: mockPullRequest() });
  expect(document.body.innerHTML).toMatchSnapshot();
});

function renderBranchLikeIcon(props: BranchLikeIconProps) {
  return render(<BranchLikeIcon {...props} />);
}
