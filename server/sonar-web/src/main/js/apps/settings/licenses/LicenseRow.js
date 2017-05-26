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
import moment from 'moment';
import LicenseStatus from './LicenseStatus';
import LicenseChangeForm from './LicenseChangeForm';

export default class LicenseRow extends React.PureComponent {
  static propTypes = {
    license: React.PropTypes.object.isRequired,
    setLicense: React.PropTypes.func.isRequired
  };

  handleSet = value => this.props.setLicense(this.props.license.key, value);

  render() {
    const { license } = this.props;

    return (
      <tr className="js-license" data-license-key={license.key}>
        <td className="text-middle"><LicenseStatus license={license} /></td>
        <td className="js-product text-middle">
          <div className={license.invalidProduct ? 'text-danger' : null}>
            {license.name || license.key}
          </div>
        </td>
        <td className="js-organization text-middle">{license.organization}</td>
        <td className="js-expiration text-middle">
          {license.expiration != null &&
            <div className={license.invalidExpiration ? 'text-danger' : null}>
              {moment(license.expiration).format('LL')}
            </div>}
        </td>
        <td className="js-type text-middle">{license.type}</td>
        <td className="js-server-id text-middle">
          <div className={license.invalidServerId ? 'text-danger' : null}>
            {license.serverId}
          </div>
        </td>
        <td className="text-right">
          <LicenseChangeForm license={license} onChange={this.handleSet} />
        </td>
      </tr>
    );
  }
}
