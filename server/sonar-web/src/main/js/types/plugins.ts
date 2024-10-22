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
  category?: string;
  description?: string;
  editionBundled?: boolean;
  homepageUrl?: string;
  issueTrackerUrl?: string;
  key: string;
  license?: string;
  name: string;
  organizationName?: string;
  organizationUrl?: string;
  termsAndConditionsUrl?: string;
}

export interface PendingPluginResult {
  installing: PendingPlugin[];
  removing: PendingPlugin[];
  updating: PendingPlugin[];
}

export interface AvailablePlugin extends Plugin {
  release: Release;
  update: Update;
}

export interface PendingPlugin extends Plugin {
  implementationBuild: string;
  version: string;
}

export interface InstalledPlugin extends PendingPlugin {
  documentationPath?: string;
  filename: string;
  hash: string;
  issueTrackerUrl?: string;
  sonarLintSupported: boolean;
  updatedAt: number;
  updates?: Update[];
}

export interface Release {
  changeLogUrl?: string;
  date: string;
  description?: string;
  version: string;
}

export interface Update {
  previousUpdates?: Update[];
  release?: Release;
  requires: Plugin[];
  status: string;
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
