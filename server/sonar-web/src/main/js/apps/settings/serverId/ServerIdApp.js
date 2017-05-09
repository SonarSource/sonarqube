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
import Helmet from 'react-helmet';
import { translate } from '../../../helpers/l10n';
import { getServerId, generateServerId } from '../../../api/settings';
import { parseError } from '../../code/utils';

export default class ServerIdApp extends React.PureComponent {
  static propTypes = {
    addGlobalErrorMessage: React.PropTypes.func.isRequired,
    closeAllGlobalMessages: React.PropTypes.func.isRequired
  };

  state = {
    loading: true,
    organization: '',
    ip: '',
    validIpAddresses: []
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchServerId();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleError(error) {
    this.setState({ loading: false });
    parseError(error).then(message => this.props.addGlobalErrorMessage(message));
  }

  fetchServerId() {
    this.setState({ loading: true });
    getServerId()
      .then(data => {
        if (this.mounted) {
          this.setState({ ...data, loading: false });
        }
      })
      .catch(error => this.handleError(error));
  }

  handleSubmit(e) {
    e.preventDefault();
    this.setState({ loading: true });
    this.props.closeAllGlobalMessages();
    generateServerId(this.state.organization, this.state.ip)
      .then(data => {
        if (this.mounted) {
          this.setState({ serverId: data.serverId, invalidServerId: false, loading: false });
        }
      })
      .catch(error => this.handleError(error));
  }

  render() {
    return (
      <div id="server-id-page" className="page page-limited">
        <Helmet title={translate('property.category.server_id')} />
        <header className="page-header">
          <h1 className="page-title">{translate('property.category.server_id')}</h1>
          {this.state.loading && <i className="spinner" />}
          <div className="page-description">{translate('server_id_configuration.information')}</div>
        </header>

        {this.state.serverId != null &&
          <div className={this.state.invalidServerId ? 'panel panel-danger' : 'panel'}>
            Server ID:
            <input
              id="server-id-result"
              className="spacer-left input-large input-clear input-code"
              type="text"
              readOnly={true}
              value={this.state.serverId}
            />
            {!!this.state.invalidServerId &&
              <span className="spacer-left">{translate('server_id_configuration.bad_key')}</span>}
          </div>}

        <div className="panel">
          <form id="server-id-form" onSubmit={e => this.handleSubmit(e)}>
            <div className="modal-field">
              <label htmlFor="server-id-organization">
                {translate('server_id_configuration.organisation.title')}
                <em className="mandatory">*</em>
              </label>
              <input
                id="server-id-organization"
                type="text"
                required={true}
                value={this.state.organization}
                disabled={this.state.loading}
                onChange={e => this.setState({ organization: e.target.value })}
              />
              <div className="modal-field-description">
                {translate('server_id_configuration.organisation.desc')}
                {'. '}
                {translate('server_id_configuration.organisation.pattern')}
              </div>
            </div>

            <div className="modal-field">
              <label htmlFor="server-id-ip">
                {translate('server_id_configuration.ip.title')}
                <em className="mandatory">*</em>
              </label>
              <input
                id="server-id-ip"
                type="text"
                required={true}
                value={this.state.ip}
                disabled={this.state.loading}
                onChange={e => this.setState({ ip: e.target.value })}
              />
              <div className="modal-field-description">
                {translate('server_id_configuration.ip.desc')}
                <ul className="list-styled">
                  {this.state.validIpAddresses.map(ip => (
                    <li key={ip} className="little-spacer-top">{ip}</li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="modal-field">
              <button disabled={this.state.loading}>
                {translate('server_id_configuration.generate_button')}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }
}
