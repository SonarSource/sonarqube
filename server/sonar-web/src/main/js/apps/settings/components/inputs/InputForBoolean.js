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
import Toggle from '../../../../components/controls/Toggle';
import { defaultInputPropTypes } from '../../propTypes';
import { translate } from '../../../../helpers/l10n';

export default class InputForBoolean extends React.PureComponent {
  static propTypes = {
    ...defaultInputPropTypes,
    value: PropTypes.oneOfType([PropTypes.bool, PropTypes.string])
  };

  render() {
    const hasValue = this.props.value != null;
    const displayedValue = hasValue ? this.props.value : false;

    return (
      <div className="display-inline-block text-top">
        <Toggle name={this.props.name} value={displayedValue} onChange={this.props.onChange} />

        {!hasValue && <span className="spacer-left note">{translate('settings.not_set')}</span>}
      </div>
    );
  }
}
