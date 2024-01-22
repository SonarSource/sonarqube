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
import { ButtonSecondary } from 'design-system';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Update } from '../../../types/plugins';

interface Props {
  disabled: boolean;
  onClick: (update: Update) => void;
  update: Update;
}

export default function PluginUpdateButton(props: Readonly<Props>) {
  const { disabled, onClick, update } = props;

  const handleClick = React.useCallback(() => {
    onClick(update);
  }, [onClick, update]);

  if (update.status !== 'COMPATIBLE' || !update.release) {
    return null;
  }
  return (
    <Tooltip overlay={translate('marketplace.requires_restart')}>
      <ButtonSecondary disabled={disabled} onClick={handleClick}>
        {translateWithParameters('marketplace.update_to_x', update.release.version)}
      </ButtonSecondary>
    </Tooltip>
  );
}
