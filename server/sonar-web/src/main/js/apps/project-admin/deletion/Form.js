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
import { deleteProject, deletePortfolio } from '../../../api/components';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class Form extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object.isRequired
  };

  static contextTypes = {
    router: PropTypes.object
  };

  state = { loading: false, modalOpen: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDeleteClick = event => {
    event.preventDefault();
    this.setState({ modalOpen: true });
  };

  closeModal = () => this.setState({ modalOpen: false });

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleSubmit = event => {
    event.preventDefault();
    this.setState({ loading: true });
    const { component } = this.props;
    const deleteMethod = component.qualifier === 'TRK' ? deleteProject : deletePortfolio;
    deleteMethod(component.key)
      .then(() => this.context.router.replace(component.qualifier === 'TRK' ? '/' : '/portfolios'))
      .catch(this.stopLoading);
  };

  handleCloseClick = (event /*: Event */) => {
    event.preventDefault();
    this.closeModal();
  };

  render() {
    const { component } = this.props;

    return (
      <div>
        <button id="delete-project" className="button-red" onClick={this.handleDeleteClick}>
          {translate('delete')}
        </button>

        {this.state.modalOpen && (
          <Modal contentLabel="project deletion" onRequestClose={this.closeModal}>
            <form onSubmit={this.handleSubmit}>
              <div className="modal-head">
                <h2>{translate('qualifier.delete.TRK')}</h2>
              </div>
              <div className="modal-body">
                <div className="js-modal-messages" />
                {translateWithParameters(
                  'project_deletion.delete_resource_confirmation',
                  component.name
                )}
              </div>
              <div className="modal-foot">
                {this.state.loading && <i className="js-modal-spinner spinner spacer-right" />}
                <button
                  id="delete-project-confirm"
                  className="button-red"
                  disabled={this.state.loading}>
                  {translate('delete')}
                </button>
                <a href="#" className="js-modal-close" onClick={this.handleCloseClick}>
                  {translate('cancel')}
                </a>
              </div>
            </form>
          </Modal>
        )}
      </div>
    );
  }
}
