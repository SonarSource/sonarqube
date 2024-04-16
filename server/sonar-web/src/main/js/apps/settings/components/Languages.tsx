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
import { PopupZLevel, SearchSelectDropdown, SubHeading } from 'design-system';
import * as React from 'react';
import { Options } from 'react-select';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { CATEGORY_OVERRIDES, LANGUAGES_CATEGORY } from '../constants';
import { getCategoryName } from '../utils';
import { AdditionalCategoryComponentProps } from './AdditionalCategories';
import CategoryDefinitionsList from './CategoryDefinitionsList';

export interface LanguagesProps extends AdditionalCategoryComponentProps {
  location: Location;
  router: Router;
}

interface SelectOption {
  label: string;
  originalValue: string;
  value: string;
}

export function Languages(props: Readonly<LanguagesProps>) {
  const { categories, component, definitions, location, router, selectedCategory } = props;
  const { availableLanguages, selectedLanguage } = getLanguages(categories, selectedCategory);

  const handleOnChange = (newOption: SelectOption) => {
    router.push({
      ...location,
      query: { ...location.query, category: newOption.originalValue },
    });
  };

  const handleLanguagesSearch = React.useCallback(
    (query: string, cb: (options: Options<SelectOption>) => void) => {
      const normalizedQuery = query.toLowerCase();

      cb(
        availableLanguages.filter(
          (lang) =>
            lang.label.toLowerCase().includes(normalizedQuery) ||
            lang.value.includes(normalizedQuery),
        ),
      );
    },
    [availableLanguages],
  );

  return (
    <>
      <SubHeading id="languages-category-title">
        {translate('property.category.languages')}
      </SubHeading>
      <div data-test="language-select">
        <SearchSelectDropdown
          defaultOptions={availableLanguages}
          controlAriaLabel={translate('property.category.languages')}
          onChange={handleOnChange}
          loadOptions={handleLanguagesSearch}
          placeholder={translate('settings.languages.select_a_language_placeholder')}
          controlSize="medium"
          zLevel={PopupZLevel.Content}
          value={availableLanguages.find((language) => language.value === selectedLanguage)}
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
      value: c.toLowerCase(),
      originalValue: c,
    }));

  let selectedLanguage = undefined;

  if (
    lowerCasedSelectedCategory !== lowerCasedLanguagesCategory &&
    availableLanguages.find((c) => c.value === lowerCasedSelectedCategory)
  ) {
    selectedLanguage = lowerCasedSelectedCategory;
  }

  return {
    availableLanguages,
    selectedLanguage,
  };
}

export default withRouter(Languages);
