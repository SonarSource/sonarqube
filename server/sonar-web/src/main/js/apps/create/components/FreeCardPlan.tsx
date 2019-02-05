/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import RadioCard, { RadioCardProps } from '../../../components/controls/RadioCard';
import { Alert } from '../../../components/ui/Alert';
import { formatPrice } from '../organization/utils';
import { translate } from '../../../helpers/l10n';

interface Props extends RadioCardProps {
  almName?: string;
  hasWarning: boolean;
}

export default function FreeCardPlan({ almName, hasWarning, ...props }: Props) {
  const showInfo = almName && props.disabled;
  const showWarning = almName && hasWarning && !props.disabled;

  return (
    <RadioCard title={translate('billing.free_plan.title')} titleInfo={formatPrice(0)} {...props}>
      <div className="spacer-left">
        <ul className="big-spacer-left note">
          <li className="little-spacer-bottom">
            {translate('billing.free_plan.all_projects_analyzed_public')}
          </li>
          <li>{translate('billing.free_plan.anyone_can_browse_source_code')}</li>
        </ul>
      </div>
      {showWarning && (
        <Alert variant="warning">
          <FormattedMessage
            defaultMessage={translate('billing.free_plan.private_repo_warning')}
            id="billing.free_plan.private_repo_warning"
            values={{ alm: almName }}
          />
        </Alert>
      )}
      {showInfo && (
        <Alert variant="info">
          <FormattedMessage
            defaultMessage={translate('billing.free_plan.not_available_info')}
            id="billing.free_plan.not_available_info"
            values={{ alm: almName }}
          />
        </Alert>
      )}
    </RadioCard>
  );
}
