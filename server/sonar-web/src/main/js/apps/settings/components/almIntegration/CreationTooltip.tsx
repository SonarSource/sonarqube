/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { withAppState } from '../../../../components/hoc/withAppState';
import { getEdition, getEditionUrl } from '../../../../helpers/editions';
import Tooltip from '../../../../sonar-ui-common/components/controls/Tooltip';
import { translate } from '../../../../sonar-ui-common/helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { EditionKey } from '../../../../types/editions';

export interface CreationTooltipProps {
  alm: AlmKeys;
  appState: T.AppState;
  children: React.ReactElement<{}>;
  preventCreation: boolean;
}

export function CreationTooltip(props: CreationTooltipProps) {
  const {
    alm,
    appState: { edition },
    children,
    preventCreation
  } = props;

  const sourceEdition = edition ? EditionKey[edition] : undefined;

  return (
    <Tooltip
      overlay={
        preventCreation ? (
          <FormattedMessage
            id="settings.almintegration.create.tooltip"
            defaultMessage={translate('settings.almintegration.create.tooltip')}
            values={{
              link: (
                <a
                  href={getEditionUrl(getEdition(EditionKey.enterprise), {
                    sourceEdition
                  })}
                  rel="noopener noreferrer"
                  target="_blank">
                  {translate('settings.almintegration.create.tooltip.link')}
                </a>
              ),
              alm: translate('alm', alm)
            }}
          />
        ) : null
      }
      mouseLeaveDelay={0.25}>
      {children}
    </Tooltip>
  );
}

export default withAppState(CreationTooltip);
