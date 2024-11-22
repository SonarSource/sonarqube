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
import { getWrappedDisplayName } from '../../../components/hoc/utils';
import { AppState } from '../../../types/appstate';
import { AppStateContext } from './AppStateContext';

export interface WithAppStateContextProps {
  appState: AppState;
}

export default function withAppStateContext<P>(
  WrappedComponent: React.ComponentType<P & WithAppStateContextProps>
) {
  return class WithAppStateContext extends React.PureComponent<
    Omit<P, keyof WithAppStateContextProps>
  > {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withAppStateContext');

    render() {
      return (
        <AppStateContext.Consumer>
          {(appState) => <WrappedComponent appState={appState} {...(this.props as P)} />}
        </AppStateContext.Consumer>
      );
    }
  };
}

export function useAppState() {
  return React.useContext(AppStateContext);
}
