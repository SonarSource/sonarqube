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
import { RadioButtonGroup } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import * as React from 'react';
import { Visibility } from '~sonar-aligned/types/component';
import { translate } from '../../helpers/l10n';

export interface VisibilitySelectorProps {
  canTurnToPrivate?: boolean;
  className?: string;
  onChange: (visibility: Visibility) => void;
  visibility?: Visibility;
  disabled?: boolean;
  loading?: boolean;
}

export default function VisibilitySelector(props: Readonly<VisibilitySelectorProps>) {
  const { className, canTurnToPrivate, visibility, disabled, loading = false } = props;
  return (
    <div className={classNames(className)}>
      <RadioButtonGroup
        id="project-visiblity-radio"
        isDisabled={disabled}
        options={Object.values(Visibility).map((v) => ({
          label: translate('visibility', v),
          value: v,
          isDisabled: (v === Visibility.Private && !canTurnToPrivate) || loading,
        }))}
        value={visibility}
        onChange={props.onChange}
      />
    </div>
  );
}
