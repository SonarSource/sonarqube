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

export default class EncryptionForm extends React.Component {
  static propTypes = {
    encryptedValue: React.PropTypes.string,
    encryptValue: React.PropTypes.func.isRequired,
    generateSecretKey: React.PropTypes.func.isRequired
  };

  state = { value: '' };

  handleEncrypt(e) {
    e.preventDefault();
    this.props.encryptValue(this.state.value);
  }

  handleGenerateNewKey(e) {
    e.preventDefault();
    this.props.generateSecretKey();
  }

  render() {
    return (
      <div id="encryption-form-container">
        <div className="spacer-bottom">
          Secret key is registered. You can encrypt any property value with the following form:
        </div>

        <form
          id="encryption-form"
          className="big-spacer-bottom"
          onSubmit={e => this.handleEncrypt(e)}>
          <input
            id="encryption-form-value"
            className="input-large"
            type="text"
            autoFocus={true}
            required={true}
            value={this.state.value}
            onChange={e => this.setState({ value: e.target.value })}
          />
          <button className="spacer-left">Encrypt</button>
        </form>

        {this.props.encryptedValue != null &&
          <div>
            Encrypted Value:{' '}
            <input
              id="encrypted-value"
              className="input-clear input-code input-super-large"
              type="text"
              readOnly={true}
              value={this.props.encryptedValue}
            />
          </div>}

        <div className="huge-spacer-top bordered-top">
          <div className="big-spacer-top spacer-bottom">
            Note that the secret key can be changed, but all the encrypted properties
            {' '}
            will have to be updated.
            {' '}
            <a href="https://redirect.sonarsource.com/doc/settings-encryption.html">
              More information
            </a>
          </div>

          <form id="encryption-new-key-form" onSubmit={e => this.handleGenerateNewKey(e)}>
            <button>Generate New Secret Key</button>
          </form>
        </div>
      </div>
    );
  }
}
