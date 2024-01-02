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
import * as React from 'react';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import BranchStatus, { BranchStatusProps } from '../BranchStatus';

const handler = new BranchesServiceMock();

beforeEach(() => {
  handler.reset();
});

it('should render ok status', async () => {
  renderBranchStatus({ branchLike: mockBranch({ status: { qualityGateStatus: 'OK' } }) });

  expect(await screen.findByText('OK')).toBeInTheDocument();
});

it('should render error status', async () => {
  renderBranchStatus({ branchLike: mockBranch({ status: { qualityGateStatus: 'ERROR' } }) });

  expect(await screen.findByText('ERROR')).toBeInTheDocument();
});

function renderBranchStatus(overrides: Partial<BranchStatusProps> = {}) {
  const defaultProps = {
    branchLike: mockBranch(),
  } as const;
  return renderComponent(<BranchStatus {...defaultProps} {...overrides} />);
}
