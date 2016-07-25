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
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { deleteProject } from '../../../../api/components';

export default class Form extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired
  };

  state = {
    confirmation: false,
    loading: false
  };

  handleDelete (e) {
    e.preventDefault();
    this.setState({ confirmation: true });
  }

  confirmDeleteClick (e) {
    e.preventDefault();
    this.setState({ loading: true });
    deleteProject(this.props.component.key).then(() => {
      window.location = window.baseUrl + '/';
    });
  }

  cancelDeleteClick (e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ confirmation: false });
  }

  renderInitial () {
    return (
        <form onSubmit={this.handleDelete.bind(this)}>
          <button id="delete-project" className="button-red">
            {translate('delete')}
          </button>
        </form>
    );
  }

  renderConfirmation () {
    return (
        <form className="panel panel-warning"
              onSubmit={this.confirmDeleteClick.bind(this)}>
          <div className="big-spacer-bottom">
            {translateWithParameters(
                'project_deletion.delete_resource_confirmation',
                this.props.component.name)}
          </div>

          <div>
            <button
                id="confirm-project-deletion"
                className="button-red"
                disabled={this.state.loading}>
              {translate('delete')}
            </button>

            {this.state.loading ? (
                <i className="spinner big-spacer-left"/>
            ) : (
                <a href="#"
                   className="big-spacer-left"
                   onClick={this.cancelDeleteClick.bind(this)}>
                  {translate('cancel')}
                </a>
            )}
          </div>
        </form>
    );
  }

  render () {
    return this.state.confirmation ?
        this.renderConfirmation() :
        this.renderInitial();
  }
}
