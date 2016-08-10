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
import BulkUpdateForm from './BulkUpdateForm';
import BulkUpdateResults from './BulkUpdateResults';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { bulkChangeKey } from '../../../api/components';
import { getComponentUrl } from '../../../helpers/urls';

export default class BulkUpdate extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired
  };

  state = {
    updating: false,
    updated: false,
    newComponentKey: null
  };

  handleSubmit (replace, by) {
    this.loadResults(replace, by);
  }

  handleConfirm () {
    this.setState({ updating: true });
    
    const { component } = this.props;
    const { replace, by } = this.state;
    bulkChangeKey(component.key, replace, by).then(r => {
      const result = r.keys.find(result => result.key === component.key);
      const newComponentKey = result != null ? result.newKey : component.key;
      this.setState({
        updating: false,
        updated: true,
        newComponentKey
      });
    });
  }

  loadResults (replace, by) {
    const { component } = this.props;
    bulkChangeKey(component.key, replace, by, true).then(r => {
      this.setState({ results: r.keys, replace, by });
    });
  }

  renderUpdating () {
    return (
        <div id="project-key-bulk-update">
          <i className="spinner"/>
        </div>
    );
  }

  renderUpdated () {
    return (
        <div id="project-key-bulk-update">
          <div className="alert alert-success">
            {translate('update_key.key_updated')}
            {' '}
            <a href={getComponentUrl(this.state.newComponentKey)}>
              {translate('check_project')}
            </a>
          </div>
        </div>
    );
  }

  render () {
    const { component } = this.props;
    const { updating, updated } = this.state;
    const { results, replace, by } = this.state;

    if (updating) {
      return this.renderUpdating();
    }

    if (updated) {
      return this.renderUpdated();
    }

    return (
        <div id="project-key-bulk-update">
          <header className="big-spacer-bottom">
            <div className="spacer-bottom">
              {translate('update_key.bulk_change_description')}
            </div>
            <div>
              {translateWithParameters(
                  'update_key.current_key_for_project_x_is_x',
                  component.name,
                  component.key
              )}
            </div>
          </header>

          <BulkUpdateForm onSubmit={this.handleSubmit.bind(this)}/>

          {results != null && (
              <BulkUpdateResults
                  results={results}
                  replace={replace}
                  by={by}
                  onConfirm={this.handleConfirm.bind(this)}/>
          )}
        </div>
    );
  }
}
