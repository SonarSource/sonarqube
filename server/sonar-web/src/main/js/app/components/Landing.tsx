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

import { Navigate, To } from 'react-router-dom';
import { getHomePageUrl, isDeploymentForAmazon, isDeploymentForCodeScan } from '../../helpers/urls';
import { AppState } from '../../types/appstate';
import { CurrentUser, isLoggedIn } from '../../types/users';
import withCurrentUserContext from './current-user/withCurrentUserContext';
import withAppStateContext from './app-state/withAppStateContext';

export interface LandingProps {
  appState: AppState;
  currentUser: CurrentUser;
}

function getRedirectURL(currentUser: any) {
  let redirectUrl: To = "";
  if (currentUser.homepage) {
    redirectUrl = getHomePageUrl(currentUser.homepage);
  } else {
    redirectUrl = '/projects';
  }
  return redirectUrl;
}

export function Landing({ appState, currentUser }: LandingProps) {
  const { whiteLabel } = appState
  let redirectUrl: To;
  if (isLoggedIn(currentUser)) {
    if (isDeploymentForCodeScan(whiteLabel)) {
      if (!currentUser.onboarded) {
        redirectUrl = '/home'
      } else {
        redirectUrl = getRedirectURL(currentUser);
      }
    } else if (isDeploymentForAmazon(whiteLabel)) {
      redirectUrl = getRedirectURL(currentUser);
    }
  } else {
    redirectUrl = '/sessions/new';
  }

  return <Navigate to={redirectUrl} replace />;
}

export default withCurrentUserContext(withAppStateContext(Landing));
