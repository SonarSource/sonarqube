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
import { groupBy, isEqual, sortBy } from 'lodash';
import * as React from 'react';
import { getSubCategoryDescription, getSubCategoryName, sanitizeTranslation } from '../utils';
import DefinitionsList from './DefinitionsList';
import EmailForm from './EmailForm';

interface Props {
  category: string;
  component?: T.Component;
  fetchValues: Function;
  settings: Array<T.Setting & { definition: T.SettingCategoryDefinition }>;
}

export default class SubCategoryDefinitionsList extends React.PureComponent<Props> {
  componentDidMount() {
    this.fetchValues();
  }

  componentDidUpdate(prevProps: Props) {
    const prevKeys = prevProps.settings.map(setting => setting.definition.key);
    const keys = this.props.settings.map(setting => setting.definition.key);
    if (prevProps.component !== this.props.component || !isEqual(prevKeys, keys)) {
      this.fetchValues();
    }
  }

  fetchValues() {
    const keys = this.props.settings.map(setting => setting.definition.key).join();
    this.props.fetchValues(keys, this.props.component && this.props.component.key);
  }

  renderEmailForm = (subCategoryKey: string) => {
    const isEmailSettings = this.props.category === 'general' && subCategoryKey === 'email';
    if (!isEmailSettings) {
      return null;
    }
    return <EmailForm />;
  };

  render() {
    const bySubCategory = groupBy(this.props.settings, setting => setting.definition.subCategory);
    const subCategories = Object.keys(bySubCategory).map(key => ({
      key,
      name: getSubCategoryName(bySubCategory[key][0].definition.category, key),
      description: getSubCategoryDescription(bySubCategory[key][0].definition.category, key)
    }));
    const sortedSubCategories = sortBy(subCategories, subCategory =>
      subCategory.name.toLowerCase()
    );
    return (
      <ul className="settings-sub-categories-list">
        {sortedSubCategories.map(subCategory => (
          <li key={subCategory.key}>
            <h2 className="settings-sub-category-name">{subCategory.name}</h2>
            {subCategory.description != null && (
              <div
                className="settings-sub-category-description markdown"
                dangerouslySetInnerHTML={{ __html: sanitizeTranslation(subCategory.description) }}
              />
            )}
            <DefinitionsList
              component={this.props.component}
              settings={bySubCategory[subCategory.key]}
            />
            {this.renderEmailForm(subCategory.key)}
          </li>
        ))}
      </ul>
    );
  }
}
