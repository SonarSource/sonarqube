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

import { Theme } from '@emotion/react';
import { QueryClient } from '@tanstack/react-query';
import { IntlShape } from 'react-intl';
import { Location, Router } from '../components/hoc/withRouter';
import { AppState } from './appstate';
import { BranchLike } from './branch-like';
import { L10nBundle } from './l10nBundle';
import { Component } from './types';
import { CurrentUser, HomePage } from './users';

export enum AdminPageExtension {
  GovernanceConsole = 'governance/views_console',
}

export interface ExtensionRegistryEntry {
  providesCSSFile: boolean;
  start: ExtensionStartMethod;
}

export type ExtensionStartMethod = (
  params: ExtensionStartMethodParameter | string,
) => ExtensionStartMethodReturnType;

export interface ExtensionOptions {
  branchLike?: BranchLike;
  component: Component;
  intl: IntlShape;
  l10nBundle: L10nBundle;
  location: Location;
  router: Router;
  theme: Theme;
}

export interface ExtensionStartMethodParameter extends Omit<ExtensionOptions, 'component'> {
  appState: AppState;
  baseUrl: string;
  component?: Component;
  currentUser: CurrentUser;
  el: HTMLElement | undefined | null;
  onBranchesChange?: (updateBranches?: boolean, updatePRs?: boolean) => void;
  onComponentChange?: (changes: Partial<Component>) => void;
  queryClient: QueryClient;
  // See SONAR-16207 and core-extension-enterprise-server/src/main/js/portfolios/components/Header.tsx
  // for more information on why we're passing this as a prop to an extension.
  updateCurrentUserHomepage: (homepage: HomePage) => void;
}

export type ExtensionStartMethodReturnType = React.ReactNode | Function | void;
