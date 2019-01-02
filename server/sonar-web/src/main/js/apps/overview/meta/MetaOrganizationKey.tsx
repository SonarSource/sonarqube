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
import { translate } from '../../../helpers/l10n';
import ClipboardButton from '../../../components/controls/ClipboardButton';

interface Props {
  organization: string;
}

export default function MetaOrganizationKey({ organization }: Props) {
  return (
    <>
      <h4 className="overview-meta-header big-spacer-top">{translate('organization_key')}</h4>
      <div className="display-flex-center">
        <input className="overview-key" readOnly={true} type="text" value={organization} />
        <ClipboardButton className="little-spacer-left" copyValue={organization} />
      </div>
    </>
  );
}
