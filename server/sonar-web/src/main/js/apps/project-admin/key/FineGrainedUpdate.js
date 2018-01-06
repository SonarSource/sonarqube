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
import UpdateKeyForm from './UpdateKeyForm';
import QualifierIcon from '../../../components/shared/QualifierIcon';

export default function FineGrainedUpdate(props) {
  const { component, modules } = props;
  const components = [component, ...modules];

  return (
    <div id="project-key-fine-grained-update">
      <table className="data zebra">
        <tbody>
          {components.map(component => (
            <tr key={component.key}>
              <td className="width-40">
                <QualifierIcon qualifier={component.qualifier} /> {component.name}
              </td>
              <td>
                <UpdateKeyForm component={component} onKeyChange={props.onKeyChange} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
