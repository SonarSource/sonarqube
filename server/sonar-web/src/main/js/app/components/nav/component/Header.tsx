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
import * as React from 'react';
import { ProjectAlmBindingResponse } from '../../../../types/alm-settings';
import { BranchLike } from '../../../../types/branch-like';
import { Component } from '../../../../types/types';
import { CurrentUser } from '../../../../types/users';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
import BranchLikeNavigation from './branch-like/BranchLikeNavigation';
import { Breadcrumb } from './Breadcrumb';

export interface HeaderProps {
  branchLikes: BranchLike[];
  component: Component;
  currentBranchLike: BranchLike | undefined;
  currentUser: CurrentUser;
  projectBinding?: ProjectAlmBindingResponse;
}

export function Header(props: HeaderProps) {
  const { branchLikes, component, currentBranchLike, currentUser, projectBinding } = props;

  return (
    <div className="sw-flex sw-flex-shrink sw-items-center">
      <Breadcrumb component={component} currentUser={currentUser} />
      {currentBranchLike && (
        <>
          <span className="slash-separator sw-ml-2" />
          <BranchLikeNavigation
            branchLikes={branchLikes}
            component={component}
            currentBranchLike={currentBranchLike}
            projectBinding={projectBinding}
          />
        </>
      )}
    </div>
  );
}

export default withCurrentUserContext(React.memo(Header));
