/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import CheckIcon from 'sonar-ui-common/components/icons/CheckIcon';
import ClearIcon from 'sonar-ui-common/components/icons/ClearIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../../../app/theme';

export interface AlmIntegrationFeatureBoxProps {
  active: boolean;
  description: React.ReactNode;
  inactiveReason?: React.ReactNode;
  name: React.ReactNode;
}

export default function AlmIntegrationFeatureBox(props: AlmIntegrationFeatureBoxProps) {
  const { active, description, inactiveReason, name } = props;

  return (
    <div
      className={classNames(
        'boxed-group-inner display-flex-start width-30 spacer-right spacer-bottom bordered',
        {
          'bg-muted': !active
        }
      )}>
      {active ? (
        <CheckIcon className="little-spacer-top spacer-right" fill={colors.green} />
      ) : (
        <ClearIcon className="little-spacer-top spacer-right" fill={colors.gray60} />
      )}
      <div className="display-flex-column abs-height-100">
        <h4>{name}</h4>

        <div className="spacer-top flex-1">{description}</div>

        <div className="spacer-top">
          {active ? (
            <em className="text-success">{translate('settings.almintegration.feature.enabled')}</em>
          ) : (
            <em className="text-muted">
              {inactiveReason || translate('settings.almintegration.feature.disabled')}
            </em>
          )}
        </div>
      </div>
    </div>
  );
}
