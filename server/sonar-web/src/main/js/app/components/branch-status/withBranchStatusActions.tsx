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
import * as React from 'react';
import { getWrappedDisplayName } from '../../../components/hoc/utils';
import { BranchStatusContext, BranchStatusContextInterface } from './BranchStatusContext';

export type WithBranchStatusActionsProps =
  | Pick<BranchStatusContextInterface, 'fetchBranchStatus'>
  | Pick<BranchStatusContextInterface, 'updateBranchStatus'>;

export default function withBranchStatusActions<P>(
  WrappedComponent: React.ComponentType<P & WithBranchStatusActionsProps>
) {
  return class WithBranchStatusActions extends React.PureComponent<
    Omit<P, keyof BranchStatusContextInterface>
  > {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withBranchStatusActions');

    render() {
      return (
        <BranchStatusContext.Consumer>
          {({ fetchBranchStatus, updateBranchStatus }) => (
            <WrappedComponent
              fetchBranchStatus={fetchBranchStatus}
              updateBranchStatus={updateBranchStatus}
              {...(this.props as P)}
            />
          )}
        </BranchStatusContext.Consumer>
      );
    }
  };
}
