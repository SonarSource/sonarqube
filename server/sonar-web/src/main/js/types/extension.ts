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
import { IntlShape } from 'react-intl';
import { Store as ReduxStore } from 'redux';
import { Location, Router } from '../components/hoc/withRouter';
import { Store } from '../store/rootReducer';
import { L10nBundle } from './l10n';
import { AppState, CurrentUser, Dict } from './types';

export enum AdminPageExtension {
  GovernanceConsole = 'governance/views_console'
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
  store: ReduxStore<Store, any>;
  el: HTMLElement | undefined | null;
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
  baseUrl: string;
  l10nBundle: L10nBundle;
}

export type ExtensionStartMethodReturnType = React.ReactNode | Function | void | undefined | null;
