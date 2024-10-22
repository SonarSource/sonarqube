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

import { DarkLabel } from '~design-system';
import AlmSettingsInstanceSelector from '../../../../components/devops-platform/AlmSettingsInstanceSelector';
import { hasMessage, translate, translateWithParameters } from '../../../../helpers/l10n';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';

interface Props {
  almInstances?: AlmSettingsInstance[];
  almKey: AlmKeys;
  onChangeConfig: (instance: AlmSettingsInstance) => void;
  selectedAlmInstance?: AlmSettingsInstance;
}

const MIN_SIZE_INSTANCES = 2;

export default function AlmSettingsInstanceDropdown(props: Readonly<Props>) {
  const { almKey, almInstances, selectedAlmInstance } = props;
  if (!almInstances || almInstances.length < MIN_SIZE_INSTANCES) {
    return null;
  }

  const almKeyTranslation = hasMessage(`alm.${almKey}.long`)
    ? `alm.${almKey}.long`
    : `alm.${almKey}`;

  return (
    <div className="sw-flex sw-flex-col sw-mb-9">
      <DarkLabel htmlFor="alm-config-selector" className="sw-mb-2">
        {translateWithParameters('alm.configuration.selector.label', translate(almKeyTranslation))}
      </DarkLabel>
      <AlmSettingsInstanceSelector
        instances={almInstances}
        onChange={props.onChangeConfig}
        initialValue={selectedAlmInstance ? selectedAlmInstance.key : undefined}
        className="sw-w-abs-400"
        inputId="alm-config-selector"
      />
    </div>
  );
}
