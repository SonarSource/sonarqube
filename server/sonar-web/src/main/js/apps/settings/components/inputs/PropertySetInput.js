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
import renderInput from './renderInput';

export default class PropertySetInput extends React.Component {
  static propTypes = {
    setting: React.PropTypes.object.isRequired
  };

  render () {
    const { setting } = this.props;
    const fieldsValues = setting.fieldsValues || [{}];

    return (
        <table className="data zebra" style={{ width: 480, marginTop: -5 }}>
          <thead>
            <tr>
              {setting.definition.fields.map(field => (
                  <th key={field.key}>
                    {field.name}
                    {field.description != null && (
                        <span className="spacer-top small">{field.description}</span>
                    )}
                  </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {fieldsValues.map((fieldValues, index) => (
                <tr key={index}>
                  {setting.definition.fields.map(field => (
                      <td key={field.key}>
                        {renderInput({ definition: field, value: fieldValues[field.key] })}
                      </td>
                  ))}
                </tr>
            ))}
          </tbody>
        </table>
    );
  }
}
