/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { withRouter } from 'react-router';
import debounce from 'lodash/debounce';
import { getFilterUrl } from './utils';
import { translate } from '../../../helpers/l10n';

class SearchFilter extends React.Component {
  static propTypes = {
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object
  }

  handleQueryChange = debounce(option => {
    const path = getFilterUrl(this.props, { search: option || null });
    this.props.router.push(path);
  }, 250);

  render () {
    return (
      <div className="projects-facet-header" data-key="search">
        <input
          type="text"
          className="input-super-large"
          placeholder={translate('projects.search')}
          onChange={event => this.handleQueryChange(event.target.value)}/>
      </div>
    );
  }
}

export default withRouter(SearchFilter);
