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

import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { LANGUAGES_CATEGORY, NEW_CODE_PERIOD_CATEGORY } from './AdditionalCategoryKeys';
import Languages from './Languages';
import NewCodePeriod from './NewCodePeriod';

export interface AdditionalCategory {
  key: string;
  name: string;
  renderComponent: (
    parentComponent: T.Component | undefined,
    selectedCategory: string
  ) => JSX.Element;
  availableGlobally: boolean;
  availableForProject: boolean;
}

export const ADDITIONAL_CATEGORIES: AdditionalCategory[] = [
  {
    key: LANGUAGES_CATEGORY,
    name: translate('property.category.languages'),
    renderComponent: getLanguagesComponent,
    availableGlobally: true,
    availableForProject: true
  },
  {
    key: NEW_CODE_PERIOD_CATEGORY,
    name: translate('settings.new_code_period.category'),
    renderComponent: getNewCodePeriodComponent,
    availableGlobally: true,
    availableForProject: false
  }
];

function getLanguagesComponent(component: any, originalCategory: string) {
  return <Languages component={component} selectedCategory={originalCategory} />;
}

function getNewCodePeriodComponent() {
  return <NewCodePeriod />;
}
