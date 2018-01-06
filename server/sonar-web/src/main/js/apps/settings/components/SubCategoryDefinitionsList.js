/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import PropTypes from 'prop-types';
import { groupBy, isEqual, sortBy } from 'lodash';
import DefinitionsList from './DefinitionsList';
import EmailForm from './EmailForm';
import { getSubCategoryName, getSubCategoryDescription } from '../utils';

export default class SubCategoryDefinitionsList extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object,
    fetchValues: PropTypes.func,
    settings: PropTypes.array.isRequired
  };

  componentDidMount() {
    this.fetchValues();
  }

  componentDidUpdate(prevProps /*: Object */) {
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

  renderEmailForm(subCategoryKey /*: string */) {
    const isEmailSettings = this.props.category === 'general' && subCategoryKey === 'email';
    if (!isEmailSettings) {
      return null;
    }
    return <EmailForm />;
  }

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
                dangerouslySetInnerHTML={{ __html: subCategory.description }}
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
