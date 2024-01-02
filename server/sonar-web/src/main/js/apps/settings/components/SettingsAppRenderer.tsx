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
import { uniqBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { Location, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { Component } from '../../../types/types';
import { CATEGORY_OVERRIDES } from '../constants';
import { getDefaultCategory } from '../utils';
import { ADDITIONAL_CATEGORIES } from './AdditionalCategories';
import AllCategoriesList from './AllCategoriesList';
import CategoryDefinitionsList from './CategoryDefinitionsList';
import PageHeader from './PageHeader';

export interface SettingsAppRendererProps {
  definitions: ExtendedSettingDefinition[];
  component?: Component;
  loading: boolean;
  location: Location;
}

export function SettingsAppRenderer(props: SettingsAppRendererProps) {
  const { definitions, component, loading, location } = props;

  const categories = React.useMemo(() => {
    return uniqBy(
      definitions.map((definition) => definition.category),
      (category) => category.toLowerCase()
    );
  }, [definitions]);

  if (loading) {
    return null;
  }

  const { query } = location;
  const defaultCategory = getDefaultCategory(categories);
  const originalCategory = (query.category as string) || defaultCategory;
  const overriddenCategory = CATEGORY_OVERRIDES[originalCategory.toLowerCase()];
  const selectedCategory = overriddenCategory || originalCategory;
  const foundAdditionalCategory = ADDITIONAL_CATEGORIES.find((c) => c.key === selectedCategory);
  const isProjectSettings = component;
  const shouldRenderAdditionalCategory =
    foundAdditionalCategory &&
    ((isProjectSettings && foundAdditionalCategory.availableForProject) ||
      (!isProjectSettings && foundAdditionalCategory.availableGlobally));

  return (
    <div id="settings-page">
      <Suggestions suggestions="settings" />
      <Helmet defer={false} title={translate('settings.page')} />
      <PageHeader component={component} definitions={definitions} />

      <div className="layout-page">
        <ScreenPositionHelper className="layout-page-side-outer">
          {({ top }) => (
            <div className="layout-page-side" style={{ top }}>
              <div className="layout-page-side-inner">
                <AllCategoriesList
                  categories={categories}
                  component={component}
                  defaultCategory={defaultCategory}
                  selectedCategory={selectedCategory}
                />
              </div>
            </div>
          )}
        </ScreenPositionHelper>

        <div className="layout-page-main">
          <div className="layout-page-main-inner">
            {/* Adding a key to force re-rendering of the category content, so that it resets the scroll position */}
            <div className="big-padded" key={selectedCategory}>
              {foundAdditionalCategory && shouldRenderAdditionalCategory ? (
                foundAdditionalCategory.renderComponent({
                  categories,
                  component,
                  definitions,
                  selectedCategory: originalCategory,
                })
              ) : (
                <CategoryDefinitionsList
                  category={selectedCategory}
                  component={component}
                  definitions={definitions}
                />
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default withRouter(SettingsAppRenderer);
