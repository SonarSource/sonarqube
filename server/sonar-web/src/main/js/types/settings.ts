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

import { Dict } from './types';

export const enum SettingsKey {
  AuditHouseKeeping = 'sonar.dbcleaner.auditHousekeeping',
  DaysBeforeDeletingInactiveBranchesAndPRs = 'sonar.dbcleaner.daysBeforeDeletingInactiveBranchesAndPRs',
  DefaultProjectVisibility = 'projects.default.visibility',
  ServerBaseUrl = 'sonar.core.serverBaseURL',
  PluginRiskConsent = 'sonar.plugins.risk.consent',
  LicenceRemainingLocNotificationThreshold = 'sonar.license.notifications.remainingLocThreshold',
  TokenMaxAllowedLifetime = 'sonar.auth.token.max.allowed.lifetime',
  QPAdminCanDisableInheritedRules = 'sonar.qualityProfiles.allowDisableInheritedRules',
  MQRMode = 'sonar.multi-quality-mode.enabled',
  CodeSuggestion = 'sonar.ai.suggestions.enabled',
}

export enum GlobalSettingKeys {
  LogoUrl = 'sonar.lf.logoUrl',
  LogoWidth = 'sonar.lf.logoWidthPx',
  EnableGravatar = 'sonar.lf.enableGravatar',
  GravatarServerUrl = 'sonar.lf.gravatarServerUrl',
  RatingGrid = 'sonar.technicalDebt.ratingGrid',
  DeveloperAggregatedInfoDisabled = 'sonar.developerAggregatedInfo.disabled',
  UpdatecenterActivated = 'sonar.updatecenter.activate',
  DisplayAnnouncementMessage = 'sonar.announcement.displayMessage',
  AnnouncementMessage = 'sonar.announcement.message',
  MainBranchName = 'sonar.projectCreation.mainBranchName',
}

export type SettingDefinitionAndValue = {
  definition: ExtendedSettingDefinition;
  settingValue?: SettingValue;
};

export type Setting = SettingValue & { definition: SettingDefinition; hasValue: boolean };
export type SettingWithCategory = Setting & { definition: ExtendedSettingDefinition };

export enum SettingType {
  STRING = 'STRING',
  TEXT = 'TEXT',
  JSON = 'JSON',
  PASSWORD = 'PASSWORD',
  BOOLEAN = 'BOOLEAN',
  FLOAT = 'FLOAT',
  INTEGER = 'INTEGER',
  LICENSE = 'LICENSE',
  LONG = 'LONG',
  SINGLE_SELECT_LIST = 'SINGLE_SELECT_LIST',
  PROPERTY_SET = 'PROPERTY_SET',
  FORMATTED_TEXT = 'FORMATTED_TEXT',
  REGULAR_EXPRESSION = 'REGULAR_EXPRESSION',
  USER_LOGIN = 'USER_LOGIN',
}
export interface SettingDefinition {
  description?: string;
  key: string;
  multiValues?: boolean;
  name?: string;
  options: string[];
  type?: SettingType;
}

export interface SettingFieldDefinition extends SettingDefinition {
  name: string;
}

export interface ExtendedSettingDefinition extends SettingDefinition {
  category: string;
  defaultValue?: string;
  deprecatedKey?: string;
  fields: SettingFieldDefinition[];
  multiValues?: boolean;
  subCategory: string;
}

export interface DefinitionV2 {
  description?: string;
  key: string;
  multiValues?: boolean;
  name: string;
  secured: boolean;
  type?: SettingType;
}

export interface SettingValueResponse {
  setSecuredSettings: string[];
  settings: SettingValue[];
}

export interface SettingValue {
  fieldValues?: Array<Dict<string>>;
  inherited?: boolean;
  key: string;
  parentFieldValues?: Array<Dict<string>>;
  parentValue?: string;
  parentValues?: string[];
  value?: string;
  values?: string[];
}
