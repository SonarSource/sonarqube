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
import { QueryClient } from '@tanstack/react-query';
import { Theme } from 'design-system';
import { IntlShape } from 'react-intl';
import { Location, Router } from '../components/hoc/withRouter';
import { AppState } from './appstate';
import { L10nBundle } from './l10nBundle';
import { Component, Dict } from './types';
import { CurrentUser, HomePage } from './users';

export enum AdminPageExtension {
  GovernanceConsole = 'governance/views_console',
}

export interface ExtensionRegistryEntry {
  start: ExtensionStartMethod;
  providesCSSFile: boolean;
}

export interface ExtensionStartMethod {
  (params: ExtensionStartMethodParameter | string): ExtensionStartMethodReturnType;
}

export interface ExtensionStartMethodParameter {
  appState: AppState;
  el: HTMLElement | undefined | null;
  component?: Component;
  onBranchesChange?: (updateBranches?: boolean, updatePRs?: boolean) => void;
  onComponentChange?: (changes: Partial<Component>) => void;
  currentUser: CurrentUser;
  intl: IntlShape;
  location: Location;
  router: Router;
  theme: {
    colors: Dict<string>;
    sizes: Dict<string>;
    rawSizes: Dict<number>;
    fonts: Dict<string>;
    zIndexes: Dict<string>;
    others: Dict<string>;
  };
  dsTheme: Theme;
  baseUrl: string;
  l10nBundle: L10nBundle;
  queryClient: QueryClient;
  // See SONAR-16207 and core-extension-enterprise-server/src/main/js/portfolios/components/Header.tsx
  // for more information on why we're passing this as a prop to an extension.
  updateCurrentUserHomepage: (homepage: HomePage) => void;
}

export type ExtensionStartMethodReturnType = React.ReactNode | Function | void | undefined | null;
