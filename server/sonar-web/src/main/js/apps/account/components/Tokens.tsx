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
import { translate } from 'sonar-ui-common/helpers/l10n';
import InstanceMessage from '../../../components/common/InstanceMessage';
import TokensForm from '../../users/components/TokensForm';

interface Props {
  login: string;
}

export default function Tokens({ login }: Props) {
  return (
    <div className="boxed-group">
      <h2>{translate('users.tokens')}</h2>
      <div className="boxed-group-inner">
        <div className="big-spacer-bottom big-spacer-right markdown">
          <InstanceMessage message={translate('my_account.tokens_description')} />
        </div>

        <TokensForm deleteConfirmation="modal" login={login} />
      </div>
    </div>
  );
}
