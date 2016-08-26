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
import sortBy from 'lodash/sortBy';
import { IndexLink } from 'react-router';
import { getCategoryName } from '../utils';

export default class CategoriesList extends React.Component {
  static propTypes = {
    categories: React.PropTypes.array.isRequired,
    selectedCategory: React.PropTypes.string.isRequired,
    defaultCategory: React.PropTypes.string.isRequired
  };

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  renderLink (category) {
    const query = {};

    if (category.key !== this.props.defaultCategory) {
      query.category = category.key.toLowerCase();
    }

    if (this.props.component) {
      query.id = this.props.component.key;
    }

    const className = category.key.toLowerCase() === this.props.selectedCategory.toLowerCase() ? 'active' : '';

    return (
        <IndexLink to={{ pathname: '/', query }} className={className} title={category.name}>
          {category.name}
        </IndexLink>
    );
  }

  render () {
    const categoriesWithName = this.props.categories.map(key => ({ key, name: getCategoryName(key) }));
    const sortedCategories = sortBy(categoriesWithName, category => category.name.toLowerCase());

    return (
        <ul className="settings-menu">
          {sortedCategories.map(category => (
              <li key={category.key}>
                {this.renderLink(category)}
              </li>
          ))}
        </ul>
    );
  }
}
