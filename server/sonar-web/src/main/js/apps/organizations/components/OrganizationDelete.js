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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';
import { getOrganizationByKey } from '../../../store/rootReducer';
import { deleteOrganization } from '../actions';

class OrganizationDelete extends React.PureComponent {
  /*:: props: {
    organization: {
      key: string,
      name: string
    },
    router: {
      replace: string => void
    },
    deleteOrganization: string => Promise<*>
  };
*/

  state = {
    deleting: false,
    loading: false
  };

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    this.setState({ loading: true });
    this.props.deleteOrganization(this.props.organization.key).then(() => {
      this.props.router.replace('/');
    });
  };

  handleOpenModal = () => {
    this.setState({ deleting: true });
  };

  handleCloseModal = () => {
    this.setState({ deleting: false });
  };

  renderModal() {
    return (
      <Modal contentLabel="modal form" onRequestClose={this.handleCloseModal}>
        <header className="modal-head">
          <h2>{translate('organization.delete')}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">{translate('organization.delete.question')}</div>

          <footer className="modal-foot">
            {this.state.loading ? (
              <i className="spinner" />
            ) : (
              <div>
                <button type="submit" className="button-red">
                  {translate('delete')}
                </button>
                <button type="reset" className="button-link" onClick={this.handleCloseModal}>
                  {translate('cancel')}
                </button>
              </div>
            )}
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    const title = translate('organization.delete');
    return (
      <div className="page page-limited">
        <Helmet title={title} />

        <header className="page-header">
          <h1 className="page-title">{title}</h1>
          <div className="page-description">{translate('organization.delete.description')}</div>
        </header>

        <div>
          <button
            className="button-red"
            disabled={this.state.loading || this.state.deleting}
            onClick={this.handleOpenModal}>
            {translate('delete')}
          </button>
          {this.state.deleting && this.renderModal()}
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = { deleteOrganization };

export default connect(null, mapDispatchToProps)(withRouter(OrganizationDelete));

export const UnconnectedOrganizationDelete = OrganizationDelete;
