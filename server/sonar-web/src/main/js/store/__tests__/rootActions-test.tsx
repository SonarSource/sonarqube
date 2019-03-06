/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { registerBranchStatus } from '../rootActions';
import { mockLongLivingBranch } from '../../helpers/testMocks';
import { registerBranchStatusAction } from '../branches';

jest.mock('../branches', () => ({
  ...require.requireActual('../branches'),
  registerBranchStatusAction: jest.fn()
}));

it('correctly dispatches actions for branches', () => {
  const dispatch = jest.fn();
  const branchLike = mockLongLivingBranch();
  const component = 'foo';
  const status = 'OK';

  registerBranchStatus(branchLike, component, status)(dispatch);
  expect(registerBranchStatusAction).toBeCalledWith(branchLike, component, status);
  expect(dispatch).toBeCalled();
});
