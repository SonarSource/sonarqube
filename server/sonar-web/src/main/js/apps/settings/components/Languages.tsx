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
import { connect } from 'react-redux';
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { getSettingsAppAllCategories, Store } from '../../../store/rootReducer';
import { getCategoryName } from '../utils';
import { AdditionalCategoryComponentProps } from './AdditionalCategories';
import { LANGUAGES_CATEGORY } from './AdditionalCategoryKeys';
import CategoryDefinitionsList from './CategoryDefinitionsList';
import { CATEGORY_OVERRIDES } from './CategoryOverrides';

export interface LanguagesProps extends AdditionalCategoryComponentProps {
  categories: string[];
  location: Location;
  router: Router;
}

interface SelectOption {
  label: string;
  originalValue: string;
  value: string;
}

export function Languages(props: LanguagesProps) {
  const { categories, component, location, router, selectedCategory } = props;
  const { availableLanguages, selectedLanguage } = getLanguages(categories, selectedCategory);

  const handleOnChange = (newOption: SelectOption) => {
    router.push({
      ...location,
      query: { ...location.query, category: newOption.originalValue }
    });
  };

  return (
    <>
      <h2 className="settings-sub-category-name">{translate('property.category.languages')}</h2>
      <div data-test="language-select">
        <Select
          className="input-large"
          onChange={handleOnChange}
          options={availableLanguages}
          placeholder={translate('settings.languages.select_a_language_placeholder')}
          value={selectedLanguage}
        />
      </div>
      {selectedLanguage && (
        <div className="settings-sub-category">
          <CategoryDefinitionsList category={selectedLanguage} component={component} />
        </div>
      )}
    </>
  );
}

function getLanguages(categories: string[], selectedCategory: string) {
  const lowerCasedLanguagesCategory = LANGUAGES_CATEGORY.toLowerCase();
  const lowerCasedSelectedCategory = selectedCategory.toLowerCase();

  const availableLanguages = categories
    .filter(c => CATEGORY_OVERRIDES[c.toLowerCase()] === lowerCasedLanguagesCategory)
    .map(c => ({
      label: getCategoryName(c),
      value: c.toLowerCase(),
      originalValue: c
    }));

  let selectedLanguage = undefined;

  if (
    lowerCasedSelectedCategory !== lowerCasedLanguagesCategory &&
    availableLanguages.find(c => c.value === lowerCasedSelectedCategory)
  ) {
    selectedLanguage = lowerCasedSelectedCategory;
  }

  return {
    availableLanguages,
    selectedLanguage
  };
}

export default withRouter(
  connect((state: Store) => ({
    categories: getSettingsAppAllCategories(state)
  }))(Languages)
);
