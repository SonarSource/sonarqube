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
import debounce from 'lodash/debounce';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class ProjectsSearch extends React.Component {
  static propTypes = {
    onSearch: React.PropTypes.func.isRequired
  };

  componentWillMount () {
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.onSearch = debounce(this.props.onSearch, 250);
  }

  handleChange () {
    const { value } = this.refs.input;
    if (value.length > 2 || value.length === 0) {
      this.onSearch(value);
    }
  }

  handleSubmit (e) {
    e.preventDefault();
    this.handleChange();
  }

  render () {
    return (
        <div className="big-spacer-bottom">
          <form onSubmit={this.handleSubmit}>
            <input
                ref="input"
                type="search"
                className="input-large"
                placeholder={translate('search_verb')}
                onChange={this.handleChange}/>
              <span className="note spacer-left">
                {translateWithParameters(
                    'my_account.projects.x_characters_min', 3)}
              </span>
          </form>
        </div>
    );
  }
}
