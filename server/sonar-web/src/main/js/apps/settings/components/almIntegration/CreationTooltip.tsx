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
import { FormattedMessage } from 'react-intl';
import withAppStateContext from '../../../../app/components/app-state/withAppStateContext';
import Tooltip from '../../../../components/controls/Tooltip';
import { getEdition, getEditionUrl } from '../../../../helpers/editions';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { AppState } from '../../../../types/appstate';
import { EditionKey } from '../../../../types/editions';

export interface CreationTooltipProps {
  alm: AlmKeys;
  appState: AppState;
  children: React.ReactElement<{}>;
  preventCreation: boolean;
}

export function CreationTooltip(props: CreationTooltipProps) {
  const {
    alm,
    appState: { edition },
    children,
    preventCreation,
  } = props;

  const sourceEdition = edition ? EditionKey[edition] : undefined;

  return (
    <Tooltip
      content={
        preventCreation ? (
          <FormattedMessage
            id="settings.almintegration.create.tooltip"
            defaultMessage={translate('settings.almintegration.create.tooltip')}
            values={{
              link: (
                <a
                  href={getEditionUrl(getEdition(EditionKey.enterprise), {
                    sourceEdition,
                  })}
                  rel="noopener noreferrer"
                  target="_blank"
                >
                  {translate('settings.almintegration.create.tooltip.link')}
                </a>
              ),
              alm: translate('alm', alm),
            }}
          />
        ) : null
      }
      mouseLeaveDelay={0.25}
    >
      {children}
    </Tooltip>
  );
}

export default withAppStateContext(CreationTooltip);
