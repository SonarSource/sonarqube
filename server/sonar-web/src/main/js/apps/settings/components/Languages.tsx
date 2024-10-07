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
import { InputSize, Select } from '@sonarsource/echoes-react';
import { SubHeading } from 'design-system';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, Router } from '~sonar-aligned/types/router';
import { translate } from '../../../helpers/l10n';
import { CATEGORY_OVERRIDES, LANGUAGES_CATEGORY } from '../constants';
import { getCategoryName } from '../utils';
import { AdditionalCategoryComponentProps } from './AdditionalCategories';
import CategoryDefinitionsList from './CategoryDefinitionsList';

export interface LanguagesProps extends AdditionalCategoryComponentProps {
  location: Location;
  router: Router;
}

export function Languages(props: Readonly<LanguagesProps>) {
  const { categories, component, definitions, location, router, selectedCategory } = props;
  const { availableLanguages, selectedLanguage } = React.useMemo(
    () => getLanguages(categories, selectedCategory),
    [categories, selectedCategory],
  );

  const handleOnChange = (selection: string | null) => {
    router.push({
      ...location,
      query: { ...location.query, category: selection ?? LANGUAGES_CATEGORY },
    });
  };

  return (
    <>
      <SubHeading id="languages-category-title">
        {translate('property.category.languages')}
      </SubHeading>
      <div data-test="language-select">
        <Select
          data={availableLanguages}
          onChange={handleOnChange}
          value={selectedLanguage ?? null /* null clears the input */}
          ariaLabelledBy="languages-category-title"
          isSearchable
          size={InputSize.Medium}
        />
      </div>
      {selectedLanguage && (
        <CategoryDefinitionsList
          category={selectedLanguage}
          component={component}
          definitions={definitions}
        />
      )}
    </>
  );
}

function getLanguages(categories: string[], selectedCategory: string) {
  const lowerCasedLanguagesCategory = LANGUAGES_CATEGORY.toLowerCase();
  const lowerCasedSelectedCategory = selectedCategory.toLowerCase();

  const availableLanguages = categories
    .filter((c) => CATEGORY_OVERRIDES[c.toLowerCase()] === lowerCasedLanguagesCategory)
    .map((c) => ({
      label: getCategoryName(c),
      value: c,
    }));

  let selectedLanguage = undefined;

  if (lowerCasedSelectedCategory !== lowerCasedLanguagesCategory) {
    const match = availableLanguages.find(
      (c) => c.value.toLowerCase() === lowerCasedSelectedCategory,
    );

    if (match) {
      selectedLanguage = match.value;
    }
  }

  return {
    availableLanguages,
    selectedLanguage,
  };
}

export default withRouter(Languages);
