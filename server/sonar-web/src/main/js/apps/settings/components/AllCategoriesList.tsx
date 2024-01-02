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
import classNames from 'classnames';
import { sortBy } from 'lodash';
import * as React from 'react';
import { NavLink } from 'react-router-dom';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { getGlobalSettingsUrl, getProjectSettingsUrl } from '../../../helpers/urls';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import { CATEGORY_OVERRIDES } from '../constants';
import { getCategoryName } from '../utils';
import { ADDITIONAL_CATEGORIES } from './AdditionalCategories';

export interface CategoriesListProps extends WithAvailableFeaturesProps {
  categories: string[];
  component?: Component;
  defaultCategory: string;
  selectedCategory: string;
}

export function CategoriesList(props: CategoriesListProps) {
  const { categories, component, defaultCategory, selectedCategory } = props;

  const categoriesWithName = categories
    .filter((key) => !CATEGORY_OVERRIDES[key.toLowerCase()])
    .map((key) => ({
      key,
      name: getCategoryName(key),
    }))
    .concat(
      ADDITIONAL_CATEGORIES.filter((c) => c.displayTab)
        .filter((c) =>
          component
            ? // Project settings
              c.availableForProject
            : // Global settings
              c.availableGlobally
        )
        .filter((c) => props.hasFeature(Feature.BranchSupport) || !c.requiresBranchSupport)
    );
  const sortedCategories = sortBy(categoriesWithName, (category) => category.name.toLowerCase());

  return (
    <ul className="side-tabs-menu">
      {sortedCategories.map((c) => {
        const category = c.key !== defaultCategory ? c.key.toLowerCase() : undefined;
        return (
          <li key={c.key}>
            <NavLink
              end={true}
              className={(_) =>
                classNames({
                  active: c.key.toLowerCase() === selectedCategory.toLowerCase(),
                })
              }
              title={c.name}
              to={
                component
                  ? getProjectSettingsUrl(component.key, category)
                  : getGlobalSettingsUrl(category)
              }
            >
              {c.name}
            </NavLink>
          </li>
        );
      })}
    </ul>
  );
}

export default withAvailableFeatures(CategoriesList);
