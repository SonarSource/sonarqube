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
export interface CurrentUser {
  isLoggedIn: boolean;
  permissions?: { global: string[] };
  usingSonarLintConnectedMode?: boolean;
  dismissedNotices: { [key: string]: boolean };
}

export interface Notice {
  key: NoticeType;
  value: boolean;
}

export enum NoticeType {
  EDUCATION_PRINCIPLES = 'educationPrinciples',
  SONARLINT_AD = 'sonarlintAd',
}

export interface LoggedInUser extends CurrentUser, UserActive {
  externalIdentity?: string;
  externalProvider?: string;
  groups: string[];
  homepage?: HomePage;
  isLoggedIn: true;
  local?: boolean;
  scmAccounts: string[];
  settings?: CurrentUserSetting[];
  sonarLintAdSeen?: boolean;
}

export type HomePage =
  | { type: 'APPLICATION'; branch: string | undefined; component: string }
  | { type: 'ISSUES' }
  | { type: 'MY_ISSUES' }
  | { type: 'MY_PROJECTS' }
  | { type: 'PORTFOLIO'; component: string }
  | { type: 'PORTFOLIOS' }
  | { type: 'PROJECT'; branch: string | undefined; component: string }
  | { type: 'PROJECTS' };

export interface CurrentUserSetting {
  key: CurrentUserSettingNames;
  value: string;
}

export type CurrentUserSettingNames =
  | 'notifications.optOut'
  | 'notifications.readDate'
  | 'tutorials.jenkins.skipBitbucketPreReqs';

export interface UserActive extends UserBase {
  active?: true;
  name: string;
}

export interface User extends UserBase {
  externalIdentity?: string;
  externalProvider?: string;
  groups?: string[];
  lastConnectionDate?: string;
  local: boolean;
  scmAccounts?: string[];
  tokensCount?: number;
}

export interface UserBase {
  active?: boolean;
  avatar?: string;
  email?: string;
  login: string;
  name?: string;
}

export function isUserActive(user: UserBase): user is UserActive {
  return user.active !== false && Boolean(user.name);
}

export function isLoggedIn(user: CurrentUser): user is LoggedInUser {
  return user.isLoggedIn;
}
