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
import * as classNames from 'classnames';
import { connect } from 'react-redux';
import { sortBy } from 'lodash';
import { IndexLink } from 'react-router';
import { getCategoryName } from '../utils';
import { getSettingsAppAllCategories, Store } from '../../../store/rootReducer';

interface Category {
  key: string;
  name: string;
}

interface Props {
  categories: string[];
  component?: T.Component;
  defaultCategory: string;
  selectedCategory: string;
}

export class CategoriesList extends React.PureComponent<Props> {
  renderLink(category: Category) {
    const { component, defaultCategory, selectedCategory } = this.props;
    const pathname = this.props.component ? '/project/settings' : '/settings';
    const query = {
      category: category.key !== defaultCategory ? category.key.toLowerCase() : undefined,
      id: component && component.key
    };
    return (
      <IndexLink
        className={classNames({
          active: category.key.toLowerCase() === selectedCategory.toLowerCase()
        })}
        title={category.name}
        to={{ pathname, query }}>
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
        {sortedCategories.map(category => (
          <li key={category.key}>{this.renderLink(category)}</li>
        ))}
      </ul>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  categories: getSettingsAppAllCategories(state)
});

export default connect(mapStateToProps)(CategoriesList);
