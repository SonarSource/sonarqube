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
// @flow
import React from 'react';
import PropTypes from 'prop-types';
import { getSettingValue, isEmptyValue, getDefaultValue } from '../utils';
import { translate } from '../../../helpers/l10n';

export default class DefinitionDefaults extends React.PureComponent {
  static propTypes = {
    setting: PropTypes.object.isRequired,
    isDefault: PropTypes.bool.isRequired,
    onReset: PropTypes.func.isRequired
  };

  handleReset(e /*: Object */) {
    e.preventDefault();
    e.target.blur();
    this.props.onReset();
  }

  render() {
    const { setting, isDefault } = this.props;
    const { definition } = setting;

    const isExplicitlySet = !isDefault && !isEmptyValue(definition, getSettingValue(setting));

    return (
      <div>
        {isDefault &&
          <div className="spacer-top note" style={{ lineHeight: '24px' }}>
            {translate('settings._default')}
          </div>}

        {isExplicitlySet &&
          <div className="spacer-top nowrap">
            <button onClick={e => this.handleReset(e)}>
              {translate('reset_verb')}
            </button>
            <span className="spacer-left note">
              {translate('default')}
              {': '}
              {getDefaultValue(setting)}
            </span>
          </div>}
      </div>
    );
  }
}
