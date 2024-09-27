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

import { Checkbox, Spinner } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { CellComponent, Table, TableRowInteractive } from 'design-system';
import * as React from 'react';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { hasMessage, translate, translateWithParameters } from '../../helpers/l10n';
import {
  useAddNotificationMutation,
  useNotificationsQuery,
  useRemoveNotificationMutation,
} from '../../queries/notifications';

interface Props {
  className?: string;
  project?: string;
}

function getDispatcherLabel(dispatcher: string, project?: string) {
  const globalMessageKey = ['notification.dispatcher', dispatcher];
  const projectMessageKey = [...globalMessageKey, 'project'];
  const shouldUseProjectMessage = project !== undefined && hasMessage(...projectMessageKey);

  return shouldUseProjectMessage ? translate(...projectMessageKey) : translate(...globalMessageKey);
}

export default function NotificationsList({ project, className = '' }: Readonly<Props>) {
  const { data, isLoading } = useNotificationsQuery();
  const { mutate: add, isPending: isPendingAdd } = useAddNotificationMutation();
  const { mutate: remove, isPending: isPendingRemove } = useRemoveNotificationMutation();
  const types = (project ? data?.perProjectTypes : data?.globalTypes) || [];
  const channels = data?.channels || [];

  const checkboxId = useCallback(
    (type: string, channel: string) => {
      return project === undefined
        ? `global-notification-${type}-${channel}`
        : `project-notification-${project}-${type}-${channel}`;
    },
    [project],
  );

  const isEnabled = useCallback(
    (type: string, channel: string) =>
      !!data?.notifications.find(
        (notification) =>
          notification.type === type &&
          notification.channel === channel &&
          notification.project === project,
      ),
    [data?.notifications, project],
  );

  const handleCheck = useCallback(
    (type: string, channel: string, checked: boolean) => {
      if (checked) {
        add({ type, channel, project });
      } else {
        remove({ type, channel, project });
      }
    },
    [add, project, remove],
  );

  return (
    <Spinner isLoading={isLoading}>
      <Table
        className={classNames('sw-w-full', className)}
        columnCount={channels.length + 1}
        header={
          <tr>
            <th className="sw-typo-semibold">{translate('notification.for')}</th>

            {channels.map((channel) => (
              <th className="sw-typo-semibold sw-text-right" key={channel}>
                <FormattedMessage
                  id="notification.by"
                  values={{ channel: <FormattedMessage id={`notification.channel.${channel}`} /> }}
                />
              </th>
            ))}
          </tr>
        }
      >
        {types.map((type) => (
          <TableRowInteractive className="sw-h-9" key={type}>
            <CellComponent className="sw-py-0 sw-border-0">
              {getDispatcherLabel(type, project)}
            </CellComponent>

            {channels.map((channel) => (
              <CellComponent className="sw-py-0 sw-border-0" key={channel}>
                <div className="sw-justify-end sw-flex sw-items-center">
                  <Checkbox
                    ariaLabel={translateWithParameters(
                      'notification.dispatcher.description_x',
                      getDispatcherLabel(type, project),
                    )}
                    isDisabled={isPendingRemove || isPendingAdd}
                    checked={isEnabled(type, channel)}
                    id={checkboxId(type, channel)}
                    onCheck={(checked) => handleCheck(type, channel, checked as boolean)}
                  />
                </div>
              </CellComponent>
            ))}
          </TableRowInteractive>
        ))}
      </Table>
    </Spinner>
  );
}
