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
import { Link } from 'react-router';
import ActionsCell from './ActionsCell';
import { translate } from '../../../helpers/l10n';

export default class TemplateHeader extends React.Component {
  static propTypes = {
    template: React.PropTypes.object.isRequired,
    loading: React.PropTypes.bool.isRequired,
    refresh: React.PropTypes.func.isRequired,
    topQualifiers: React.PropTypes.array.isRequired
  };

  render () {
    const { template } = this.props;

    return (
        <header id="project-permissions-header" className="page-header">
          <div className="note spacer-bottom">
            <Link to="/permission_templates" className="text-muted">
              {translate('permission_templates.page')}
            </Link>
          </div>

          <h1 className="page-title">
            {template.name}
          </h1>

          {this.props.loading && (
              <i className="spinner"/>
          )}

          <div className="pull-right">
            <ActionsCell
                permissionTemplate={this.props.template}
                topQualifiers={this.props.topQualifiers}
                refresh={this.props.refresh}
                fromDetails={true}/>
          </div>
        </header>
    );
  }
}
