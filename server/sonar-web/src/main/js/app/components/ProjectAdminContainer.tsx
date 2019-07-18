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
import * as React from 'react';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import A11ySkipTarget from './a11y/A11ySkipTarget';

interface Props {
  children: JSX.Element;
  branchLike?: T.BranchLike;
  branchLikes: T.BranchLike[];
  component: T.Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onBranchesChange: () => void;
  onComponentChange: (changes: {}) => void;
}

export default class ProjectAdminContainer extends React.PureComponent<Props> {
  componentDidMount() {
    this.checkPermissions();
  }

  componentDidUpdate() {
    this.checkPermissions();
  }

  checkPermissions() {
    if (!this.isProjectAdmin()) {
      handleRequiredAuthorization();
    }
  }

  isProjectAdmin() {
    const { configuration } = this.props.component;
    return configuration != null && configuration.showSettings;
  }

  render() {
    if (!this.isProjectAdmin()) {
      return null;
    }

    const { children, ...props } = this.props;
    return (
      <>
        <A11ySkipTarget anchor="admin_main" />
        {React.cloneElement(children, props)}
      </>
    );
  }
}
