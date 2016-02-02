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

import GlobalNotifications from './GlobalNotifications';
import ProjectNotifications from './ProjectNotifications';
import { translate } from '../../../helpers/l10n';

export default function Notifications ({ globalNotifications, projectNotifications, onAddProject, onRemoveProject }) {
  const channels = globalNotifications[0].channels.map(c => c.id);

  return (
      <div className="page page-limited">
        <p className="big-spacer-bottom">
          {translate('notification.dispatcher.information')}
        </p>
        <form id="notif_form" method="post" action={`/account/update_notifications`}>
          <div className="columns columns-overflow-visible">
            <div className="column-half">
              <GlobalNotifications
                  notifications={globalNotifications}
                  channels={channels}/>
            </div>

            <div className="column-half">
              <ProjectNotifications
                  notifications={projectNotifications}
                  channels={channels}
                  onAddProject={onAddProject}
                  onRemoveProject={onRemoveProject}/>
            </div>
          </div>

          <p className="big-spacer-top panel panel-vertical bordered-top text-right">
            <button id="submit-notifications" type="submit">
              {translate('my_profile.notifications.submit')}
            </button>
          </p>
        </form>
      </div>
  );
}
