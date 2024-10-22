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

import styled from '@emotion/styled';
import { uniqBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { LargeCenteredLayout, PageContentFontWrapper, themeBorder } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location } from '~sonar-aligned/types/router';
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
  component?: Component;
  definitions: ExtendedSettingDefinition[];
  loading: boolean;
  location: Location;
}

function SettingsAppRenderer(props: Readonly<SettingsAppRendererProps>) {
  const { definitions, component, loading, location } = props;

  const categories = React.useMemo(() => {
    return uniqBy(
      definitions.map((definition) => definition.category),
      (category) => category.toLowerCase(),
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
    <LargeCenteredLayout id="settings-page">
      <Helmet defer={false} title={translate('settings.page')} />

      <PageContentFontWrapper className="sw-my-8">
        <PageHeader component={component} definitions={definitions} />

        <div className="sw-typo-default sw-flex sw-items-stretch sw-justify-between">
          <div className="sw-min-w-abs-250">
            <AllCategoriesList
              categories={categories}
              component={component}
              defaultCategory={defaultCategory}
              selectedCategory={selectedCategory}
            />
          </div>

          {/* Adding a key to force re-rendering of the category content, so that it resets the scroll position */}
          <StyledBox
            className="it__settings_list sw-flex-1 sw-p-6 sw-min-w-0"
            key={selectedCategory}
          >
            {shouldRenderAdditionalCategory ? (
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
          </StyledBox>
        </div>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withRouter(SettingsAppRenderer);

const StyledBox = styled.div`
  border: ${themeBorder('default', 'subnavigationBorder')};
  margin-left: -1px;
`;
