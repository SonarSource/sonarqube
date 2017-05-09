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
import GenerateSecretKeyForm from './GenerateSecretKeyForm';
import EncryptionForm from './EncryptionForm';
import { translate } from '../../../helpers/l10n';

export default class EncryptionApp extends React.PureComponent {
  static propTypes = {
    loading: React.PropTypes.bool.isRequired,
    secretKeyAvailable: React.PropTypes.bool,
    secretKey: React.PropTypes.string,
    encryptedValue: React.PropTypes.string,

    checkSecretKey: React.PropTypes.func.isRequired,
    generateSecretKey: React.PropTypes.func.isRequired,
    encryptValue: React.PropTypes.func.isRequired,
    startGeneration: React.PropTypes.func.isRequired
  };

  componentDidMount() {
    this.props.checkSecretKey();
  }

  render() {
    return (
      <div id="encryption-page" className="page page-limited">
        <Helmet title={translate('property.category.security.encryption')} />
        <header className="page-header">
          <h1 className="page-title">{translate('property.category.security.encryption')}</h1>
          {this.props.loading && <i className="spinner" />}
        </header>

        {!this.props.loading &&
          !this.props.secretKeyAvailable &&
          <GenerateSecretKeyForm
            secretKey={this.props.secretKey}
            generateSecretKey={this.props.generateSecretKey}
          />}

        {this.props.secretKeyAvailable &&
          <EncryptionForm
            encryptedValue={this.props.encryptedValue}
            encryptValue={this.props.encryptValue}
            generateSecretKey={this.props.startGeneration}
          />}
      </div>
    );
  }
}
