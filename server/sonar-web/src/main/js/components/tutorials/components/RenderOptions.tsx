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

import { ToggleButton } from '~design-system';
import { translate } from '../../../helpers/l10n';

export interface RenderOptionsProps {
  checked: string | undefined;
  label?: string;
  onCheck: (checked: string) => void;
  optionLabelKey: string;
  options: string[];
  titleLabelKey?: string;
}

export default function RenderOptions({
  checked,
  label,
  onCheck,
  optionLabelKey,
  options,
  titleLabelKey,
}: RenderOptionsProps) {
  const onChange = (checked: string) => {
    onCheck(checked);
  };

  return (
    <div className="sw-mt-4">
      {titleLabelKey && <label className="sw-block sw-mb-1">{translate(titleLabelKey)}</label>}

      <ToggleButton
        label={label}
        onChange={onChange}
        options={options.map((build) => ({
          label: translate(optionLabelKey, build),
          value: build,
        }))}
        value={checked}
      />
    </div>
  );
}
