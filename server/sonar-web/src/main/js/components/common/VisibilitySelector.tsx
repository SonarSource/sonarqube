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
import classNames from 'classnames';
import { RadioButton } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { Visibility } from '../../types/component';

export interface VisibilitySelectorProps {
  canTurnToPrivate?: boolean;
  className?: string;
  onChange: (visibility: Visibility) => void;
  showDetails?: boolean;
  visibility?: Visibility;
  disabled?: boolean;
  loading?: boolean;
}

export default function VisibilitySelector(props: VisibilitySelectorProps) {
  const { className, canTurnToPrivate, visibility, showDetails, disabled, loading = false } = props;
  return (
    <div className={classNames(className)}>
      {Object.values(Visibility).map((v) => (
        <RadioButton
          className={`sw-mr-10 it__visibility-${v}`}
          key={v}
          value={v}
          checked={v === visibility}
          onCheck={props.onChange}
          disabled={disabled || (v === Visibility.Private && !canTurnToPrivate) || loading}
        >
          <div>
            {translate('visibility', v)}
            {showDetails && (
              <p className="note">{translate('visibility', v, 'description.long')}</p>
            )}
          </div>
        </RadioButton>
      ))}
    </div>
  );
}
