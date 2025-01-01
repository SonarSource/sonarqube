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

import { FormattedMessage } from 'react-intl';
import { LightLabel, RequiredIcon } from '~design-system';
import { translate } from '../../helpers/l10n';

export interface MandatoryFieldsExplanationProps {
  className?: string;
}

export default function MandatoryFieldsExplanation({ className }: MandatoryFieldsExplanationProps) {
  return (
    <LightLabel aria-hidden className={className}>
      <FormattedMessage
        id="fields_marked_with_x_required"
        defaultMessage={translate('fields_marked_with_x_required')}
        values={{ star: <RequiredIcon className="sw-m-0" /> }}
      />
    </LightLabel>
  );
}
