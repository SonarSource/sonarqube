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
import * as React from 'react';
import { Alert } from '../../components/ui/Alert';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import { isApplication } from '../../types/component';
import { Component } from '../../types/types';

export interface NonAdminPagesContainerProps {
  children: JSX.Element;
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onBranchesChange: () => void;
  onComponentChange: (changes: {}) => void;
}

export default function NonAdminPagesContainer(props: NonAdminPagesContainerProps) {
  const { children, component } = props;

  /*
   * Catch Applications for which the user does not have access to all child projects
   * and prevent displaying whatever page was requested.
   * This doesn't apply to admin pages (those are not within this container)
   */
  if (isApplication(component.qualifier) && !component.canBrowseAllChildProjects) {
    return (
      <div className="page page-limited display-flex-justify-center">
        <Alert
          className="it__alert-no-access-all-child-project max-width-60 huge-spacer-top"
          display="block"
          variant="error">
          <p>{translate('application.cannot_access_all_child_projects1')}</p>
          <br />
          <p>{translate('application.cannot_access_all_child_projects2')}</p>
        </Alert>
      </div>
    );
  }

  return React.cloneElement(children, props);
}
