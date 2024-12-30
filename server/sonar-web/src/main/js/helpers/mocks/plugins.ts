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

import {
  AvailablePlugin,
  InstalledPlugin,
  PendingPlugin,
  Plugin,
  Release,
  Update,
} from '../../types/plugins';

export function mockPlugin(overrides: Partial<Plugin> = {}): Plugin {
  return {
    key: 'sonar-foo',
    name: 'Sonar Foo',
    ...overrides,
  };
}

export function mockPendingPlugin(overrides: Partial<PendingPlugin> = {}): PendingPlugin {
  return {
    key: 'sonar-foo',
    name: 'Sonar Foo',
    version: '1.0',
    implementationBuild: '1.0.0.1234',
    ...overrides,
  };
}

export function mockInstalledPlugin(overrides: Partial<InstalledPlugin> = {}): InstalledPlugin {
  return {
    key: 'sonar-bar',
    name: 'Sonar Bar',
    version: '1.0',
    implementationBuild: '1.0.0.1234',
    filename: 'sonar-bar-1.0.jar',
    hash: 'hash',
    sonarLintSupported: false,
    updatedAt: 100,
    ...overrides,
  };
}

export function mockAvailablePlugin(overrides: Partial<AvailablePlugin> = {}): AvailablePlugin {
  return {
    release: mockRelease(),
    update: mockUpdate(),
    ...mockPlugin(),
    ...overrides,
  };
}

export function mockRelease(overrides: Partial<Release> = {}): Release {
  return {
    date: '2020-01-01',
    version: '8.2',
    ...overrides,
  };
}

export function mockUpdate(overrides: Partial<Update> = {}): Update {
  return {
    status: 'available',
    requires: [],
    ...overrides,
  };
}
