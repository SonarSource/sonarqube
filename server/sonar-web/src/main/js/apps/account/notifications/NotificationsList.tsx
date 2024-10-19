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

import { Checkbox } from '@sonarsource/echoes-react';
import { CellComponent, TableRowInteractive } from 'design-system';
import * as React from 'react';
import { hasMessage, translate, translateWithParameters } from '../../../helpers/l10n';
import {
  Notification,
  NotificationGlobalType,
  NotificationProjectType,
} from '../../../types/notifications';

interface Props {
  channels: string[];
  checkboxId: (type: string, channel: string) => string;
  notifications: Notification[];
  onAdd: (n: Notification) => void;
  onRemove: (n: Notification) => void;
  project?: boolean;
  types: (NotificationGlobalType | NotificationProjectType)[];
}

export default function NotificationsList({
  channels,
  checkboxId,
  notifications,
  onAdd,
  onRemove,
  project,
  types,
}: Readonly<Props>) {
  const isEnabled = (type: string, channel: string) =>
    !!notifications.find(
      (notification) => notification.type === type && notification.channel === channel,
    );

  const handleCheck = (type: string, channel: string, checked: boolean) => {
    if (checked) {
      onAdd({ type, channel });
    } else {
      onRemove({ type, channel });
    }
  };

  const getDispatcherLabel = (dispatcher: string) => {
    const globalMessageKey = ['notification.dispatcher', dispatcher];
    const projectMessageKey = [...globalMessageKey, 'project'];
    const shouldUseProjectMessage = project && hasMessage(...projectMessageKey);

    return shouldUseProjectMessage
      ? translate(...projectMessageKey)
      : translate(...globalMessageKey);
  };

  return types.map((type) => (
    <TableRowInteractive className="sw-h-9" key={type}>
      <CellComponent className="sw-py-0 sw-border-0">{getDispatcherLabel(type)}</CellComponent>

      {channels.map((channel) => (
        <CellComponent className="sw-py-0 sw-border-0" key={channel}>
          <div className="sw-justify-end sw-flex sw-items-center">
            <Checkbox
              ariaLabel={translateWithParameters(
                'notification.dispatcher.description_x',
                getDispatcherLabel(type),
              )}
              checked={isEnabled(type, channel)}
              id={checkboxId(type, channel)}
              onCheck={(checked) => handleCheck(type, channel, checked as boolean)}
            />
          </div>
        </CellComponent>
      ))}
    </TableRowInteractive>
  ));
}
