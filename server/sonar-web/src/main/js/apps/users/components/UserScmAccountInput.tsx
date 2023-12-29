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
import { DestructiveIcon, InputField, TrashIcon } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  idx: number;
  scmAccount: string;
  onChange: (idx: number, scmAccount: string) => void;
  onRemove: (idx: number) => void;
}

export default function UserScmAccountInput(props: Props) {
  const { idx, scmAccount } = props;

  const inputAriaLabel = scmAccount.trim()
    ? translateWithParameters('users.create_user.scm_account_x', scmAccount)
    : translate('users.create_user.scm_account_new');

  return (
    <div className="it__scm-account sw-flex sw-mb-2">
      <InputField
        className="sw-mr-1"
        size="full"
        maxLength={255}
        onChange={(event) => {
          props.onChange(idx, event.currentTarget.value);
        }}
        type="text"
        aria-label={inputAriaLabel}
        value={scmAccount}
      />
      <DestructiveIcon
        Icon={TrashIcon}
        aria-label={translateWithParameters('remove_x', inputAriaLabel)}
        onClick={() => props.onRemove(idx)}
        stopPropagation={false}
      />
    </div>
  );
}
