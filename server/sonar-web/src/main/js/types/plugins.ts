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
export interface Plugin {
  key: string;
  name: string;
  category?: string;
  description?: string;
  editionBundled?: boolean;
  license?: string;
  organizationName?: string;
  homepageUrl?: string;
  organizationUrl?: string;
  issueTrackerUrl?: string;
  termsAndConditionsUrl?: string;
}

export interface PendingPluginResult {
  installing: PendingPlugin[];
  updating: PendingPlugin[];
  removing: PendingPlugin[];
}

export interface AvailablePlugin extends Plugin {
  release: Release;
  update: Update;
}

export interface PendingPlugin extends Plugin {
  version: string;
  implementationBuild: string;
}

export interface InstalledPlugin extends PendingPlugin {
  documentationPath?: string;
  issueTrackerUrl?: string;
  filename: string;
  hash: string;
  sonarLintSupported: boolean;
  updatedAt: number;
  updates?: Update[];
}

export interface Release {
  version: string;
  date: string;
  description?: string;
  changeLogUrl?: string;
}

export interface Update {
  status: string;
  release?: Release;
  requires: Plugin[];
  previousUpdates?: Update[];
}

export enum PluginType {
  Bundled = 'BUNDLED',
  External = 'EXTERNAL',
}

export enum RiskConsent {
  Accepted = 'ACCEPTED',
  NotAccepted = 'NOT_ACCEPTED',
  Required = 'REQUIRED',
}

export function isAvailablePlugin(plugin: Plugin): plugin is AvailablePlugin {
  return (plugin as any).release !== undefined;
}

export function isInstalledPlugin(plugin: Plugin): plugin is InstalledPlugin {
  return isPendingPlugin(plugin) && (plugin as any).updatedAt !== undefined;
}

export function isPendingPlugin(plugin: Plugin): plugin is PendingPlugin {
  return (plugin as any).version !== undefined;
}
