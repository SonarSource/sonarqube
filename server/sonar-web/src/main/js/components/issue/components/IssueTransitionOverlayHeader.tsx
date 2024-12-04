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

import { ButtonIcon, ButtonVariety, IconArrowLeft, IconX } from '@sonarsource/echoes-react';
import { translate } from '../../../helpers/l10n';

interface Props {
  onBack: VoidFunction;
  onClose: VoidFunction;
  selected: boolean;
}

export default function IssueTransitionOverlayHeader({
  onBack,
  onClose,
  selected,
}: Readonly<Props>) {
  if (selected) {
    return (
      <div className="sw-flex sw-items-center sw-gap-2 sw-px-3 sw-mb-1">
        <ButtonIcon
          Icon={IconArrowLeft}
          ariaLabel={translate('go_back')}
          className="sw-flex sw-justify-center sw-p-0"
          onClick={onBack}
          variety={ButtonVariety.DefaultGhost}
        />
        <span className="sw-font-semibold">
          {translate('issue.transition.go_back_change_status')}
        </span>
      </div>
    );
  }

  return (
    <div className="sw-flex sw-justify-between sw-items-center sw-px-3 sw-mb-1">
      <span className="sw-font-semibold">{translate('issue.transition.status_change')}</span>
      <ButtonIcon
        Icon={IconX}
        ariaLabel={translate('close')}
        className="sw-flex sw-justify-center sw-p-0"
        onClick={onClose}
        variety={ButtonVariety.DefaultGhost}
      />
    </div>
  );
}
