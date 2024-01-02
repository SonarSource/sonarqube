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
import { cloneDeep } from 'lodash';
import { mockAppState } from '../../helpers/testMocks';
import { AppState } from '../../types/appstate';
import { Extension, NavigationComponent } from '../../types/types';
import {
  getComponentNavigation,
  getGlobalNavigation,
  getMarketplaceNavigation,
  getSettingsNavigation,
} from '../navigation';

jest.mock('../navigation');

const defaultComponentNavigation: NavigationComponent = {
  name: 'foo',
  key: 'foo',
  breadcrumbs: [],
};

export class NavigationServiceMock {
  componentNavigation: NavigationComponent;

  constructor() {
    this.componentNavigation = cloneDeep(defaultComponentNavigation);

    jest.mocked(getComponentNavigation).mockImplementation(this.handleGetComponentNavigation);
    jest.mocked(getMarketplaceNavigation).mockImplementation(this.handleGetMarketplaceNavigation);
    jest.mocked(getSettingsNavigation).mockImplementation(this.handleGetSettingsNavigation);
    jest.mocked(getGlobalNavigation).mockImplementation(this.handleGetGlobalNavigation);
  }

  setComponentNavigation = (componentNavigation: NavigationComponent) => {
    this.componentNavigation = cloneDeep(componentNavigation);
  };

  handleGetComponentNavigation = (): Promise<NavigationComponent> => {
    return Promise.resolve(this.componentNavigation);
  };

  handleGetMarketplaceNavigation(): Promise<{ serverId: string; ncloc: number }> {
    return Promise.resolve({ serverId: 'foo', ncloc: 0 });
  }

  handleGetSettingsNavigation(): Promise<{
    extensions: Extension[];
    showUpdateCenter: boolean;
  }> {
    return Promise.resolve({ extensions: [], showUpdateCenter: false });
  }

  handleGetGlobalNavigation(): Promise<AppState> {
    return Promise.resolve(mockAppState());
  }

  reset = () => {
    this.componentNavigation = cloneDeep(defaultComponentNavigation);
  };
}
