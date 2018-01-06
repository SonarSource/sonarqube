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
import CreateView from '../views/CreateView';
import { translate } from '../../../helpers/l10n';
import { CallbackType } from '../propTypes';

export default class Header extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    ready: PropTypes.bool.isRequired,
    refresh: CallbackType
  };

  static contextTypes = {
    router: PropTypes.object
  };

  componentWillMount() {
    this.handleCreateClick = this.handleCreateClick.bind(this);
  }

  handleCreateClick(e) {
    e.preventDefault();
    const { organization } = this.props;

    new CreateView({ organization })
      .on('done', r => {
        this.props.refresh().then(() => {
          const pathname = organization
            ? `/organizations/${organization.key}/permission_templates`
            : '/permission_templates';
          this.context.router.push({
            pathname,
            query: { id: r.permissionTemplate.id }
          });
        });
      })
      .render();
  }

  render() {
    return (
      <header id="project-permissions-header" className="page-header">
        <h1 className="page-title">{translate('permission_templates.page')}</h1>

        {!this.props.ready && <i className="spinner" />}

        <div className="page-actions">
          <button onClick={this.handleCreateClick}>{translate('create')}</button>
        </div>

        <p className="page-description">{translate('permission_templates.page.description')}</p>
      </header>
    );
  }
}
