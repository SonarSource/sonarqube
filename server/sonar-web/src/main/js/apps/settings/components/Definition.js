/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import shallowCompare from 'react-addons-shallow-compare';
import DefinitionInput from './inputs/Input';
import { getPropertyName, getPropertyDescription } from '../utils';
import { translateWithParameters } from '../../../helpers/l10n';

export default class Definition extends React.Component {
  static propTypes = {
    component: React.PropTypes.object,
    setting: React.PropTypes.object.isRequired
  };

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  render () {
    const { setting } = this.props;
    const { definition } = setting;
    const propertyName = getPropertyName(definition);

    return (
        <div className="settings-definition" data-key={definition.key}>
          <div className="settings-definition-left">
            <h3 className="settings-definition-name" title={propertyName}>
              {propertyName}
            </h3>

            <div className="settings-definition-description markdown note spacer-top"
                 dangerouslySetInnerHTML={{ __html: getPropertyDescription(definition) }}/>

            <div className="settings-definition-key note little-spacer-top">
              {translateWithParameters('settings.key_x', definition.key)}
            </div>
          </div>

          <div className="settings-definition-right">
            <DefinitionInput setting={setting}/>
          </div>
        </div>
    );
  }
}
