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
import React from 'react';
import PropTypes from 'prop-types';
import CreationModal from './views/CreationModal';
import { translate } from '../../../helpers/l10n';

export default class Header extends React.PureComponent {
  static propTypes = {
    onCreate: PropTypes.func.isRequired
  };

  handleCreateClick(e) {
    e.preventDefault();
    e.target.blur();
    new CreationModal({
      onCreate: this.props.onCreate
    }).render();
  }

  render() {
    return (
      <header className="page-header">
        <h1 className="page-title">{translate('project_links.page')}</h1>
        <div className="page-actions">
          <button id="create-project-link" onClick={this.handleCreateClick.bind(this)}>
            {translate('create')}
          </button>
        </div>
        <div className="page-description">{translate('project_links.page.description')}</div>
      </header>
    );
  }
}
