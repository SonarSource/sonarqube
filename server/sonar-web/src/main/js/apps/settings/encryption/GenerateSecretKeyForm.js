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

export default class GenerateSecretKeyForm extends React.Component {
  static propTypes = {
    secretKey: React.PropTypes.string,
    generateSecretKey: React.PropTypes.func.isRequired
  };

  handleSubmit(e) {
    e.preventDefault();
    this.props.generateSecretKey();
  }

  render() {
    return (
      <div id="generate-secret-key-form-container">
        {this.props.secretKey != null
          ? <div>
              <div className="big-spacer-bottom">
                <h3 className="spacer-bottom">Secret Key</h3>
                <input
                  id="secret-key"
                  className="input-large"
                  type="text"
                  readOnly={true}
                  value={this.props.secretKey}
                />
              </div>

              <h3 className="spacer-bottom">How To Use</h3>

              <ul className="list-styled markdown">
                <li className="spacer-bottom">
                  Store the secret key in the file
                  {' '}
                  <code>~/.sonar/sonar-secret.txt</code>
                  {' '}
                  of the server. This file can
                  be relocated by defining the property <code>sonar.secretKeyPath</code>{' '}
                  in <code>conf/sonar.properties</code>
                </li>
                <li className="spacer-bottom">
                  Restrict access to this file by making it readable and by owner only
                </li>
                <li className="spacer-bottom">
                  Restart the server if the property
                  {' '}
                  <code>sonar.secretKeyPath</code>
                  {' '}
                  has been set or changed.
                </li>
                <li className="spacer-bottom">
                  Copy this file on all the machines that execute code inspection. Define the
                  {' '}
                  property <code>sonar.secretKeyPath</code> on those machines if the path is not
                  {' '}
                  <code>~/.sonar/sonar-secret.txt</code>.
                </li>
                <li>
                  For each property that you want to encrypt, generate the encrypted value and
                  {' '}
                  replace the original value wherever it is stored
                  {' '}
                  (configuration files, command lines).
                </li>
              </ul>
            </div>
          : <div>
              <p className="spacer-bottom">
                Secret key is required to be able to encrypt properties.
                {' '}
                <a href="https://redirect.sonarsource.com/doc/settings-encryption.html">
                  More information
                </a>
              </p>

              <form id="generate-secret-key-form" onSubmit={e => this.handleSubmit(e)}>
                <button>Generate Secret Key</button>
              </form>
            </div>}
      </div>
    );
  }
}
