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
import { sortBy } from 'lodash';
import { IndexLink } from 'react-router';
import { getCategoryName } from '../utils';

/*::
type Category = {
  key: string,
  name: string
};
*/

/*::
type Props = {
  categories: Category[],
  component?: { key: string },
  defaultCategory: string,
  selectedCategory: string
};
*/

export default class CategoriesList extends React.PureComponent {
  /*:: rops: Props; */

  renderLink(category /*: Category */) {
    const query /*: Object */ = {};

    if (category.key !== this.props.defaultCategory) {
      query.category = category.key.toLowerCase();
    }

    if (this.props.component) {
      query.id = this.props.component.key;
    }

    const className =
      category.key.toLowerCase() === this.props.selectedCategory.toLowerCase() ? 'active' : '';

    const pathname = this.props.component ? '/project/settings' : '/settings';

    return (
      <IndexLink to={{ pathname, query }} className={className} title={category.name}>
        {category.name}
      </IndexLink>
    );
  }

  render() {
    const categoriesWithName = this.props.categories.map(key => ({
      key,
      name: getCategoryName(key)
    }));
    const sortedCategories = sortBy(categoriesWithName, category => category.name.toLowerCase());

    return (
      <ul className="side-tabs-menu">
        {sortedCategories.map(category => <li key={category.key}>{this.renderLink(category)}</li>)}
      </ul>
    );
  }
}
