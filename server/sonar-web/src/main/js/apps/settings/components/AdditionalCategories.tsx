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
import { translate } from '../../../helpers/l10n';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { Component } from '../../../types/types';
import {
  AI_CODE_FIX_CATEGORY,
  ANALYSIS_SCOPE_CATEGORY,
  AUTHENTICATION_CATEGORY,
  EMAIL_NOTIFICATION_CATEGORY,
  LANGUAGES_CATEGORY,
  MODE_CATEGORY,
} from '../constants';
import AiCodeFixAdmin from './ai-codefix/AiCodeFixAdminCategory';
import { AnalysisScope } from './AnalysisScope';
import Authentication from './authentication/Authentication';
import EmailNotification from './email-notification/EmailNotification';
import Languages from './Languages';
import { Mode } from './Mode';

export interface AdditionalCategoryComponentProps {
  categories: string[];
  component: Component | undefined;
  definitions: ExtendedSettingDefinition[];
  selectedCategory: string;
}

export interface AdditionalCategory {
  availableForProject: boolean;
  availableGlobally: boolean;
  displayTab: boolean;
  key: string;
  name: string;
  renderComponent: (props: AdditionalCategoryComponentProps) => React.ReactNode;
  requiresBranchSupport?: boolean;
}

export const ADDITIONAL_CATEGORIES: AdditionalCategory[] = [
  {
    key: LANGUAGES_CATEGORY,
    name: translate('property.category.languages'),
    renderComponent: getLanguagesComponent,
    availableGlobally: true,
    availableForProject: true,
    displayTab: true,
  },
  {
    key: ANALYSIS_SCOPE_CATEGORY,
    name: translate('property.category.exclusions'),
    renderComponent: getAnalysisScopeComponent,
    availableGlobally: true,
    availableForProject: true,
    displayTab: false,
  },
  {
    key: AI_CODE_FIX_CATEGORY,
    name: translate('property.category.aicodefix'),
    renderComponent: getAiCodeFixComponent,
    availableGlobally: true,
    availableForProject: false,
    displayTab: true,
  },
  {
    key: AUTHENTICATION_CATEGORY,
    name: translate('property.category.authentication'),
    renderComponent: getAuthenticationComponent,
    availableGlobally: true,
    availableForProject: false,
    displayTab: false,
  },
  {
    key: EMAIL_NOTIFICATION_CATEGORY,
    name: translate('email_notification.category'),
    renderComponent: getEmailNotificationComponent,
    availableGlobally: true,
    availableForProject: false,
    displayTab: true,
  },
  {
    key: MODE_CATEGORY,
    name: translate('settings.mode.title'),
    renderComponent: getModeComponent,
    availableGlobally: true,
    availableForProject: false,
    displayTab: true,
  },
];

function getLanguagesComponent(props: AdditionalCategoryComponentProps) {
  return <Languages {...props} />;
}

function getAnalysisScopeComponent(props: AdditionalCategoryComponentProps) {
  return <AnalysisScope {...props} />;
}

function getAiCodeFixComponent(props: AdditionalCategoryComponentProps) {
  return <AiCodeFixAdmin {...props} />;
}

function getAuthenticationComponent(props: AdditionalCategoryComponentProps) {
  return <Authentication {...props} />;
}

function getEmailNotificationComponent() {
  return <EmailNotification />;
}

function getModeComponent() {
  return <Mode />;
}
