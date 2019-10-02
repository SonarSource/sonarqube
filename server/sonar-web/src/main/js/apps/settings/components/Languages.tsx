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
import { connect } from 'react-redux';
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { getSettingsAppAllCategories, Store } from '../../../store/rootReducer';
import { getCategoryName } from '../utils';
import { LANGUAGES_CATEGORY } from './AdditionalCategoryKeys';
import CategoryDefinitionsList from './CategoryDefinitionsList';
import { CATEGORY_OVERRIDES } from './CategoryOverrides';

export interface LanguagesProps {
  categories: string[];
  component?: T.Component;
  location: Location;
  selectedCategory: string;
  router: Router;
}

interface LanguagesState {
  availableLanguages: SelectOption[];
  selectedLanguage: string | undefined;
}

interface SelectOption {
  label: string;
  originalValue: string;
  value: string;
}

export class Languages extends React.PureComponent<LanguagesProps, LanguagesState> {
  constructor(props: LanguagesProps) {
    super(props);

    this.state = {
      availableLanguages: [],
      selectedLanguage: undefined
    };
  }

  componentDidMount() {
    const { selectedCategory, categories } = this.props;
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

    this.setState({
      availableLanguages,
      selectedLanguage
    });
  }

  handleOnChange = (newOption: SelectOption) => {
    this.setState({ selectedLanguage: newOption.value });

    const { location, router } = this.props;

    router.push({
      ...location,
      query: { ...location.query, category: newOption.originalValue }
    });
  };

  render() {
    const { component } = this.props;
    const { availableLanguages, selectedLanguage } = this.state;

    return (
      <>
        <h2 className="settings-sub-category-name">{translate('property.category.languages')}</h2>
        <div data-test="language-select">
          <Select
            className="input-large"
            onChange={this.handleOnChange}
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
}

export default withRouter(
  connect((state: Store) => ({
    categories: getSettingsAppAllCategories(state)
  }))(Languages)
);
