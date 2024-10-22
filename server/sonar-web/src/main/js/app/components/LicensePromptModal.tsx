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

import { Link } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { Modal } from '~design-system';
import { translate } from '../../helpers/l10n';

interface Props {
  onClose: () => void;
}

export default function LicensePromptModal({ onClose }: Readonly<Props>) {
  return (
    <Modal
      body={
        <FormattedMessage
          defaultMessage={translate('license.prompt.description')}
          id="license.prompt.description"
          values={{
            url: (
              <Link onClick={onClose} to="/admin/extension/license/app">
                {translate('license.prompt.link')}
              </Link>
            ),
          }}
        />
      }
      headerTitle={translate('license.prompt.title')}
      onClose={onClose}
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
