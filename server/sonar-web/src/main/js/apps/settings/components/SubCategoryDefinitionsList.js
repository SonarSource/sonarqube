/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import groupBy from 'lodash/groupBy';
import sortBy from 'lodash/sortBy';
import DefinitionsList from './DefinitionsList';
import { getSubCategoryName, getSubCategoryDescription } from '../utils';

export default class SubCategoryDefinitionsList extends React.Component {
  static propTypes = {
    component: React.PropTypes.object,
    definitions: React.PropTypes.array.isRequired
  };

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  render () {
    const bySubCategory = groupBy(this.props.definitions, 'subCategory');
    const subCategories = Object.keys(bySubCategory).map(key => ({
      key,
      name: getSubCategoryName(bySubCategory[key][0].category, key),
      description: getSubCategoryDescription(bySubCategory[key][0].category, key)
    }));
    const sortedSubCategories = sortBy(subCategories, subCategory => subCategory.name.toLowerCase());

    return (
        <ul className="settings-sub-categories-list">
          {sortedSubCategories.map(subCategory => (
              <li key={subCategory.key}>
                <h3 className="settings-sub-category-name">{subCategory.name}</h3>
                {subCategory.description != null && (
                    <div className="settings-sub-category-description markdown">
                      {subCategory.description}
                    </div>
                )}
                <DefinitionsList definitions={bySubCategory[subCategory.key]} component={this.props.component}/>
              </li>
          ))}
        </ul>
    );
  }
}
