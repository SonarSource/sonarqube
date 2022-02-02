/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import { sortBy } from 'lodash';
import * as React from 'react';
import { IndexLink } from 'react-router';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { getGlobalSettingsUrl, getProjectSettingsUrl } from '../../../helpers/urls';
import { AppState, Component } from '../../../types/types';
import { getCategoryName } from '../utils';
import { ADDITIONAL_CATEGORIES } from './AdditionalCategories';
import CATEGORY_OVERRIDES from './CategoryOverrides';

export interface CategoriesListProps {
  appState: AppState;
  categories: string[];
  component?: Component;
  defaultCategory: string;
  selectedCategory: string;
}

export function CategoriesList(props: CategoriesListProps) {
  const { appState, categories, component, defaultCategory, selectedCategory } = props;

  const categoriesWithName = categories
    .filter(key => !CATEGORY_OVERRIDES[key.toLowerCase()])
    .map(key => ({
      key,
      name: getCategoryName(key)
    }))
    .concat(
      ADDITIONAL_CATEGORIES.filter(c => c.displayTab)
        .filter(c =>
          component
            ? // Project settings
              c.availableForProject
            : // Global settings
              c.availableGlobally
        )
        .filter(c => appState.branchesEnabled || !c.requiresBranchesEnabled)
    );
  const sortedCategories = sortBy(categoriesWithName, category => category.name.toLowerCase());

  return (
    <ul className="side-tabs-menu">
      {sortedCategories.map(c => {
        const category = c.key !== defaultCategory ? c.key.toLowerCase() : undefined;
        return (
          <li key={c.key}>
            <IndexLink
              className={classNames({
                active: c.key.toLowerCase() === selectedCategory.toLowerCase()
              })}
              title={c.name}
              to={
                component
                  ? getProjectSettingsUrl(component.key, category)
                  : getGlobalSettingsUrl(category)
              }>
              {c.name}
            </IndexLink>
          </li>
        );
      })}
    </ul>
  );
}

export default withAppStateContext(CategoriesList);
