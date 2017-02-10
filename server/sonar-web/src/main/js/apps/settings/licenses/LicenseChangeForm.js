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
import LicenseValueView from './LicenseValueView';
import { translate } from '../../../helpers/l10n';

export default class LicenseChangeForm extends React.Component {
  static propTypes = {
    license: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired
  };

  onClick (e) {
    e.preventDefault();
    e.target.blur();

    const { license, onChange } = this.props;

    new LicenseValueView({
      productName: license.name || license.key,
      value: license.value,
      onChange
    }).render();
  }

  render () {
    return (
        <button className="js-change" onClick={e => this.onClick(e)}>{translate('update_verb')}</button>
    );
  }
}
