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
// @flow
import React from 'react';
import ViewSelect from './ViewSelect';
import { translate } from '../../../helpers/l10n';

export default class PageHeader extends React.Component {
  props: {
    loading: boolean,
    onViewChange: (string) => void,
    total?: number,
    view: string
  };

  render() {
    return (
      <header className="page-header">
        <ViewSelect onChange={this.props.onViewChange} view={this.props.view} />

        <div className="page-actions projects-page-actions">
          {!!this.props.loading && <i className="spinner spacer-right" />}

          {this.props.total != null &&
            <span>
              <strong id="projects-total">{this.props.total}</strong>
              {' '}
              {translate('projects._projects')}
            </span>}
        </div>
      </header>
    );
  }
}
