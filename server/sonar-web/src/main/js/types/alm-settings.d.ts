/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
declare namespace T {
  export interface AlmSettingsBinding {
    key: string;
  }

  export interface AlmSettingsInstance {
    alm: string;
    key: string;
    url?: string;
  }

  export interface AlmSettingsBindingDefinitions {
    azure: AzureBindingDefinition[];
    github: GithubBindingDefinition[];
  }

  export interface GithubBindingDefinition extends AlmSettingsBinding {
    url: string;
    appId: string;
    privateKey: string;
  }

  export interface AzureBindingDefinition extends AlmSettingsBinding {
    personalAccessToken: string;
  }

  export interface ProjectAlmBinding {
    key: string;
    repository?: string;
  }

  export interface AzureProjectAlmBinding {
    almSetting: string;
    project: string;
  }

  export interface GithubProjectAlmBinding {
    almSetting: string;
    project: string;
    repository: string;
  }
}
