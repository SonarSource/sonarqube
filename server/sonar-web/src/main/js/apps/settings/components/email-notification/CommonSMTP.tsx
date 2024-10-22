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

import { BasicSeparator } from '~design-system';
import { translate } from '../../../../helpers/l10n';
import { EmailNotificationFormField } from './EmailNotificationFormField';
import { EmailNotificationGroupProps, HOST, PORT, SECURITY_PROTOCOL } from './utils';

export function CommonSMTP(props: Readonly<EmailNotificationGroupProps>) {
  const { configuration, onChange } = props;

  return (
    <div className="sw-pt-6">
      <EmailNotificationFormField
        description={translate('email_notification.form.host.description')}
        id={HOST}
        onChange={(value) => onChange({ host: value })}
        name={translate('email_notification.form.host')}
        required
        value={configuration.host}
      />
      <BasicSeparator />
      <EmailNotificationFormField
        description={translate('email_notification.form.port.description')}
        id={PORT}
        onChange={(value) => onChange({ port: value })}
        name={translate('email_notification.form.port')}
        required
        type="number"
        value={configuration.port}
      />
      <BasicSeparator />
      <EmailNotificationFormField
        description={translate('email_notification.form.security_protocol.description')}
        id={SECURITY_PROTOCOL}
        onChange={(value) => onChange({ securityProtocol: value })}
        name={translate('email_notification.form.security_protocol')}
        options={['NONE', 'STARTTLS', 'SSLTLS']}
        required
        type="select"
        value={configuration.securityProtocol}
      />
      <BasicSeparator />
    </div>
  );
}
