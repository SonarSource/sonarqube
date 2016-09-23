/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';

import NotificationsList from './NotificationsList';
import { translate } from '../../../helpers/l10n';

export default function GlobalNotifications ({ notifications, channels }) {
  return (
      <section>
        <h2 className="spacer-bottom">
          {translate('my_profile.overall_notifications.title')}
        </h2>

        <table className="form">
          <thead>
            <tr>
              <th></th>
              {channels.map(channel => (
                  <th key={channel} className="text-center">
                    <h4>{translate('notification.channel', channel)}</h4>
                  </th>
              ))}
            </tr>
          </thead>

          <NotificationsList
              notifications={notifications}
              checkboxId={(d, c) => `global_notifs_${d}_${c}`}
              checkboxName={(d, c) => `global_notifs[${d}.${c}]`}/>
        </table>
      </section>
  );
}
