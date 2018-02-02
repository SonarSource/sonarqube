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
import * as React from 'react';
import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';
import ComponentsHeader from './ComponentsHeader';
import { Component as IComponent } from '../types';
import { BranchLike } from '../../../app/types';

interface Props {
  baseComponent?: IComponent;
  branchLike?: BranchLike;
  components: IComponent[];
  rootComponent: IComponent;
  selected?: IComponent;
}

export default function Components(props: Props) {
  const { baseComponent, branchLike, components, rootComponent, selected } = props;
  return (
    <table className="data zebra">
      <ComponentsHeader baseComponent={baseComponent} rootComponent={rootComponent} />
      {baseComponent && (
        <tbody>
          <Component
            branchLike={branchLike}
            component={baseComponent}
            key={baseComponent.key}
            rootComponent={rootComponent}
          />
          <tr className="blank">
            <td colSpan={8}>&nbsp;</td>
          </tr>
        </tbody>
      )}
      <tbody>
        {components.length ? (
          components.map((component, index, list) => (
            <Component
              branchLike={branchLike}
              canBrowse={true}
              component={component}
              key={component.key}
              previous={index > 0 ? list[index - 1] : undefined}
              rootComponent={rootComponent}
              selected={component === selected}
            />
          ))
        ) : (
          <ComponentsEmpty />
        )}
      </tbody>
    </table>
  );
}
