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
import { Spinner } from '@sonarsource/echoes-react';
import { SubTitle } from 'design-system/lib';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useGetEmailConfiguration } from '../../../../queries/system';
import EmailNotificationConfiguration from './EmailNotificationConfiguration';

export default function EmailNotification() {
  const { data: configuration, isLoading } = useGetEmailConfiguration();

  return (
    <div className="sw-p-6">
      <SubTitle as="h3">
        <FormattedMessage id="email_notification.header" />
      </SubTitle>
      <FormattedMessage id="email_notification.description" />
      <Spinner isLoading={isLoading}>
        <EmailNotificationConfiguration emailConfiguration={configuration ?? null} />
      </Spinner>
    </div>
  );
}
