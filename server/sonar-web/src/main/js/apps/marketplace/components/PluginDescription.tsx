/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { Plugin } from '../../../api/plugins';

interface Props {
  plugin: Plugin;
}

const PluginDescription = (props: Props) => {
  return (
    <td className="text-top width-25 big-spacer-right">
      <div>
        <strong className="js-plugin-name">{props.plugin.name}</strong>
        {props.plugin.category && (
          <span className="js-plugin-category badge spacer-left">{props.plugin.category}</span>
        )}
      </div>
      <div className="js-plugin-description little-spacer-top">{props.plugin.description}</div>
    </td>
  );
};

export default PluginDescription;
