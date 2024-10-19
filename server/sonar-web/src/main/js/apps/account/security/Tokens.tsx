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

import { Heading } from '@sonarsource/echoes-react';
import * as React from 'react';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { translate } from '../../../helpers/l10n';
import TokensForm from '../../users/components/TokensForm';

interface Props {
  login: string;
}

export default function Tokens({ login }: Readonly<Props>) {
  return (
    <>
      <Heading as="h1" hasMarginBottom>
        {translate('my_account.security')}
      </Heading>

      <div className="sw-typo-lg sw-mb-4 sw-mr-4">
        <InstanceMessage message={translate('my_account.tokens_description')} />
      </div>

      <TokensForm deleteConfirmation="modal" login={login} displayTokenTypeInput />
    </>
  );
}
