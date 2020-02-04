/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  ALM_INTEGRATION,
  ANALYSIS_SCOPE_CATEGORY,
  LANGUAGES_CATEGORY,
  NEW_CODE_PERIOD_CATEGORY,
  PULL_REQUEST_DECORATION_BINDING_CATEGORY
} from './AdditionalCategoryKeys';
import AlmIntegration from './almIntegration/AlmIntegration';
import { AnalysisScope } from './AnalysisScope';
import Languages from './Languages';
import NewCodePeriod from './NewCodePeriod';
import PullRequestDecorationBinding from './pullRequestDecorationBinding/PRDecorationBinding';

export interface AdditionalCategoryComponentProps {
  component: T.Component | undefined;
  selectedCategory: string;
}

export interface AdditionalCategory {
  availableGlobally: boolean;
  availableForProject: boolean;
  displayTab: boolean;
  key: string;
  name: string;
  renderComponent: (props: AdditionalCategoryComponentProps) => React.ReactNode;
  requiresBranchesEnabled?: boolean;
}

export const ADDITIONAL_CATEGORIES: AdditionalCategory[] = [
  {
    key: LANGUAGES_CATEGORY,
    name: translate('property.category.languages'),
    renderComponent: getLanguagesComponent,
    availableGlobally: true,
    availableForProject: true,
    displayTab: true
  },
  {
    key: NEW_CODE_PERIOD_CATEGORY,
    name: translate('settings.new_code_period.category'),
    renderComponent: getNewCodePeriodComponent,
    availableGlobally: true,
    availableForProject: false,
    displayTab: true
  },
  {
    key: ANALYSIS_SCOPE_CATEGORY,
    name: translate('property.category.exclusions'),
    renderComponent: getAnalysisScopeComponent,
    availableGlobally: true,
    availableForProject: true,
    displayTab: false
  },
  {
    key: ALM_INTEGRATION,
    name: translate('property.category.almintegration'),
    renderComponent: getAlmIntegrationComponent,
    availableGlobally: true,
    availableForProject: false,
    displayTab: false
  },
  {
    key: PULL_REQUEST_DECORATION_BINDING_CATEGORY,
    name: translate('settings.pr_decoration.binding.category'),
    renderComponent: getPullRequestDecorationBindingComponent,
    availableGlobally: false,
    availableForProject: true,
    displayTab: true,
    requiresBranchesEnabled: true
  }
];

function getLanguagesComponent(props: AdditionalCategoryComponentProps) {
  return <Languages {...props} />;
}

function getNewCodePeriodComponent() {
  return <NewCodePeriod />;
}

function getAnalysisScopeComponent(props: AdditionalCategoryComponentProps) {
  return <AnalysisScope {...props} />;
}

function getAlmIntegrationComponent(props: AdditionalCategoryComponentProps) {
  return <AlmIntegration {...props} />;
}

function getPullRequestDecorationBindingComponent(props: AdditionalCategoryComponentProps) {
  return props.component && <PullRequestDecorationBinding component={props.component} />;
}
