/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { SettingDefinitionAndValue } from '../../../types/settings';
import { Component } from '../../../types/types';
import Definition from './Definition';

interface Props {
  component?: Component;
  scrollToDefinition: (element: HTMLLIElement) => void;
  settings: SettingDefinitionAndValue[];
  onUpdate?: () => void;
}

export default function DefinitionsList(props: Props) {
  const { component, settings } = props;
  return (
    <ul className="settings-definitions-list">
      {settings.map((setting) => (
        <li
          key={setting.definition.key}
          data-key={setting.definition.key}
          ref={props.scrollToDefinition}
        >
          <Definition
            component={component}
            definition={setting.definition}
            initialSettingValue={setting.settingValue}
            onUpdate={props.onUpdate}
          />
        </li>
      ))}
    </ul>
  );
}
