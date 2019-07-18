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
import handleRequiredAuthentication from 'sonar-ui-common/helpers/handleRequiredAuthentication';
import { isLoggedIn } from '../../helpers/users';
import { getWrappedDisplayName } from './utils';
import { withCurrentUser } from './withCurrentUser';

export function whenLoggedIn<P>(WrappedComponent: React.ComponentType<P>) {
  class Wrapper extends React.Component<P & { currentUser: T.CurrentUser }> {
    static displayName = getWrappedDisplayName(WrappedComponent, 'whenLoggedIn');

    componentDidMount() {
      if (!isLoggedIn(this.props.currentUser)) {
        handleRequiredAuthentication();
      }
    }

    render() {
      if (isLoggedIn(this.props.currentUser)) {
        return <WrappedComponent {...this.props} />;
      } else {
        return null;
      }
    }
  }

  return withCurrentUser(Wrapper);
}
