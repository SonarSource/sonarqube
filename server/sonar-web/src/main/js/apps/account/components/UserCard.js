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

import Avatar from '../../../components/shared/avatar';
import { translate } from '../../../helpers/l10n';

const UserCard = ({ user }) => (
    <div className="pull-left big-spacer-right abs-width-240">
      <div className="panel panel-white">
        <div className="text-center">
          <div id="avatar" className="big-spacer-bottom">
            <Avatar email={user.email} size={100}/>
          </div>
          <h2 id="name" className="text-ellipsis" title={user.name}>{user.name}</h2>
          <p id="login" className="note text-ellipsis" title={user.login}>{user.login}</p>
          <div className="text-center spacer-top">
            <p id="email" className="text-ellipsis" title={user.email}>{user.email}</p>
          </div>
        </div>

        <div className="big-spacer-top">
          <h3 className="text-center">{translate('my_profile.groups')}</h3>
          <ul id="groups">
            {user.groups.map(group => (
                <li key={group} className="text-ellipsis" title={group}>{group}</li>
            ))}
          </ul>
        </div>

        <div className="big-spacer-top">
          <h3 className="text-center">{translate('my_profile.scm_accounts')}</h3>
          <ul id="scm-accounts">
            {user.scmAccounts.map(scmAccount => (
                <li key={scmAccount} className="text-ellipsis" title={scmAccount}>{scmAccount}</li>
            ))}
          </ul>
        </div>
      </div>
    </div>
);

export default UserCard;
