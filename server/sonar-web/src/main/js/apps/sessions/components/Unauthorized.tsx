/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { Link } from 'react-router';
import { translate } from '../../../helpers/l10n';

interface Props {
  location: {
    query: {
      message: string;
    };
  };
}

export default function Unauthorized(props: Props) {
  const { message } = props.location.query;

  return (
    <div id="bd" className="page-wrapper-simple">
      <div id="nonav" className="page-simple">
        <div className="text-center">
          <p id="unauthorized">{translate('unauthorized.message')}</p>

          {!!message && (
            <p className="spacer-top">
              {translate('unauthorized.reason')} {message}
            </p>
          )}

          <div className="big-spacer-top">
            <Link to="/">{translate('layout.home')}</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
