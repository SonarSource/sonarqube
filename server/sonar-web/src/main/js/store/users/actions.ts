/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Dispatch } from 'redux';
import * as api from '../../api/users';
import { CurrentUser, HomePage } from '../../app/types';

export const RECEIVE_CURRENT_USER = 'RECEIVE_CURRENT_USER';
export const RECEIVE_USER = 'RECEIVE_USER';
export const SKIP_ONBOARDING = 'SKIP_ONBOARDING';
export const SET_HOMEPAGE = 'SET_HOMEPAGE';

export const receiveCurrentUser = (user: CurrentUser) => ({
  type: RECEIVE_CURRENT_USER,
  user
});

export const receiveUser = (user: any) => ({
  type: RECEIVE_USER,
  user
});

export const skipOnboarding = () => ({ type: SKIP_ONBOARDING });

export const fetchCurrentUser = () => (dispatch: Dispatch<any>) => {
  return api.getCurrentUser().then(user => dispatch(receiveCurrentUser(user)));
};

export const setHomePage = (homepage: HomePage) => (dispatch: Dispatch<any>) => {
  api.setHomePage(homepage).then(
    () => {
      dispatch({ type: SET_HOMEPAGE, homepage });
    },
    () => {}
  );
};
