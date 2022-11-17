/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import AlmSettingsInstanceSelector from '../../../components/devops-platform/AlmSettingsInstanceSelector';
import { translate } from '../../../helpers/l10n';
import { AlmSettingsInstance } from '../../../types/alm-settings';

export interface AlmSettingsInstanceDropdownProps {
  almInstances?: AlmSettingsInstance[];
  selectedAlmInstance?: AlmSettingsInstance;
  onChangeConfig: (instance: AlmSettingsInstance) => void;
}

export default function AlmSettingsInstanceDropdown(props: AlmSettingsInstanceDropdownProps) {
  const { almInstances, selectedAlmInstance } = props;
  return (
    <>
      {almInstances && almInstances.length > 1 ? (
        <div className="display-flex-column huge-spacer-bottom">
          <label htmlFor="alm-config-selector" className="spacer-bottom">
            {translate('alm.configuration.selector.label')}
          </label>
          <AlmSettingsInstanceSelector
            instances={almInstances}
            onChange={props.onChangeConfig}
            initialValue={selectedAlmInstance ? selectedAlmInstance.key : undefined}
            classNames="abs-width-400"
            inputId="alm-config-selector"
          />
        </div>
      ) : null}
    </>
  );
}
