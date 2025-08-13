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

import { sortBy } from 'lodash';
import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import { SubnavigationGroup, SubnavigationItem } from '~design-system';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import { getGlobalSettingsUrl, getProjectSettingsUrl } from '../../../helpers/urls';
import { AppState } from '../../../types/appstate';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import { AI_CODE_FIX_CATEGORY, CATEGORY_OVERRIDES } from '../constants';
import { getCategoryName } from '../utils';
import { ADDITIONAL_CATEGORIES } from './AdditionalCategories';
import { ALL_CUSTOMER_CATEGORIES } from './AllCustomerCategories';

export interface CategoriesListProps extends WithAvailableFeaturesProps {
  appState: AppState;
  categories: string[];
  component?: Component;
  defaultCategory: string;
  selectedCategory: string;
}

function CategoriesList(props: Readonly<CategoriesListProps>) {
  const { categories, component, defaultCategory, selectedCategory } = props;

  const navigate = useNavigate();

  const openCategory = React.useCallback(
    (category: string | undefined) => {
      const url = component
        ? getProjectSettingsUrl(component.key, category)
        : getGlobalSettingsUrl(category);

      navigate(url);
    },
    [component, navigate],
  );

  console.log(`Rendering CategoriesList with categories: ${categories.join(', ')}`);

  const { canAdmin } = props.appState;
  let categoriesWithName;
  if (canAdmin) {
    categoriesWithName = categories
      .filter((key) => CATEGORY_OVERRIDES[key.toLowerCase()] === undefined)
      .map((key) => ({
        key,
        name: getCategoryName(key),
      }))
      .concat(
        ADDITIONAL_CATEGORIES.filter((c) => {
          const availableForCurrentMenu = component
            ? // Project settings
              c.availableForProject
            : // Global settings
              c.availableGlobally;

          return (
            c.displayTab &&
            availableForCurrentMenu &&
            (props.hasFeature(Feature.BranchSupport) || !c.requiresBranchSupport) &&
            (props.hasFeature(Feature.FixSuggestions) || c.key !== AI_CODE_FIX_CATEGORY)
          );
        }),
      );
  } else {
    categoriesWithName = props.categories
      .filter((key) => ALL_CUSTOMER_CATEGORIES[key.toLowerCase()])
      .map((key) => ({
        key,
        name: getCategoryName(key),
      }))
      .concat(
        ADDITIONAL_CATEGORIES.filter((c) => c.displayTab).filter(
          (c) => ALL_CUSTOMER_CATEGORIES[c.key.toLowerCase()],
        ),
      );
  }
  const sortedCategories = sortBy(categoriesWithName, (category) => category.name.toLowerCase());

  console.log(
    'Sorted categories:',
    sortedCategories.map((c) => c.name),
  );

  return (
    <SubnavigationGroup
      as="nav"
      aria-label={translate('settings.page')}
      className="sw-box-border it__subnavigation_menu"
    >
      {sortedCategories.map((c) => {
        const category = c.key !== defaultCategory ? c.key.toLowerCase() : undefined;
        const isActive = c.key.toLowerCase() === selectedCategory.toLowerCase();
        return (
          <SubnavigationItem
            active={isActive}
            ariaCurrent={isActive}
            onClick={() => openCategory(category)}
            key={c.key}
          >
            {c.name}
          </SubnavigationItem>
        );
      })}
    </SubnavigationGroup>
  );
}

export default withAppStateContext(withAvailableFeatures(CategoriesList));
