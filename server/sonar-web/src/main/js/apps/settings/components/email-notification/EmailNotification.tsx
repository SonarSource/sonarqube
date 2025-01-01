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

import { Spinner, Text } from '@sonarsource/echoes-react';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { SubTitle } from '~design-system';
import { useGetEmailConfiguration } from '../../../../queries/system';
import EmailNotificationConfiguration from './EmailNotificationConfiguration';
import EmailNotificationOverview from './EmailNotificationOverview';

export default function EmailNotification() {
  const [isEditing, setIsEditing] = React.useState(false);
  const { data: configuration, isLoading } = useGetEmailConfiguration();

  return (
    <div className="sw-p-6">
      <SubTitle as="h3">
        <FormattedMessage id="email_notification.header" />
      </SubTitle>
      <Text>
        <FormattedMessage id="email_notification.description" />
      </Text>
      <Spinner isLoading={isLoading}>
        {configuration == null || isEditing ? (
          <EmailNotificationConfiguration
            emailConfiguration={configuration ?? null}
            onCancel={() => setIsEditing(false)}
            onSubmitted={() => setIsEditing(false)}
          />
        ) : (
          <EmailNotificationOverview
            onEditClicked={() => setIsEditing(true)}
            emailConfiguration={configuration}
          />
        )}
      </Spinner>
    </div>
  );
}
