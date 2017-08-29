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
import PropTypes from 'prop-types';
import LicenseRowContainer from './LicenseRowContainer';
import { translate } from '../../../helpers/l10n';

export default class LicensesList extends React.PureComponent {
  static propTypes = {
    licenses: PropTypes.array.isRequired,
    fetchLicenses: PropTypes.func.isRequired
  };

  componentDidMount() {
    this.props.fetchLicenses().catch(() => {
      /* do nothing */
    });
  }

  render() {
    return (
      <table className="data zebra zebra-hover" style={{ tableLayout: 'fixed' }}>
        <thead>
          <tr>
            <th width={40}>&nbsp;</th>
            <th>{translate('licenses.list.product')}</th>
            <th width={150}>{translate('licenses.list.organization')}</th>
            <th width={150}>{translate('licenses.list.expiration')}</th>
            <th width={150}>{translate('licenses.list.type')}</th>
            <th width={150}>{translate('licenses.list.server')}</th>
            <th width={80}>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {this.props.licenses.map(licenseKey => (
            <LicenseRowContainer key={licenseKey} licenseKey={licenseKey} />
          ))}
        </tbody>
      </table>
    );
  }
}
