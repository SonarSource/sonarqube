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
import { Outlet } from 'react-router-dom';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { Component } from '../../types/types';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';
import withComponentContext from './componentContext/withComponentContext';

interface Props {
  component: Component;
}

export class ProjectAdminContainer extends React.PureComponent<Props> {
  mounted = false;

  componentDidMount() {
    // We need to check permissions *after* the parent ComponentContainer is updated.
    // This is why we wrap checkPermission() in a setTimeout().
    //
    // We want to prevent an edge-case where if you navigate from an admin component page
    // to another component page where you have "read" access but no admin access, and
    // then hit the browser's Back button, you will get redirected to the login page.
    //
    // The reason is that this component will get mounted *before* the parent
    // ComponentContainer is *updated*. The parent component still has the last component
    // stored in state, the one where the user might not have admin access. It will detect
    // the change in URL and trigger a refresh of its state, but this comes too late; the
    // checkPermission() here will already have triggered (because mounts happen before updates)
    // and redirected the user. Wrapping it in a setTimeout() allows the parent component
    // to refresh, unmounting this component, and only triggering the redirect if it makes sense.
    //
    // See https://sonarsource.atlassian.net/browse/SONAR-19437
    setTimeout(this.checkPermissions, 0);
    this.mounted = true;
  }

  componentDidUpdate() {
    this.checkPermissions();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkPermissions = () => {
    if (this.mounted && !this.isProjectAdmin()) {
      handleRequiredAuthorization();
    }
  };

  isProjectAdmin = () => {
    const { configuration } = this.props.component;
    return configuration != null && configuration.showSettings;
  };

  render() {
    if (!this.isProjectAdmin()) {
      return null;
    }

    return (
      <main>
        <A11ySkipTarget anchor="admin_main" />
        <Outlet />
      </main>
    );
  }
}

export default withComponentContext(ProjectAdminContainer);
