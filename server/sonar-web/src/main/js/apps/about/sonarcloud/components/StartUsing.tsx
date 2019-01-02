/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import ChevronRightIcon from '../../../../components/icons-components/ChevronRightcon';

export default function StartUsing() {
  return (
    <div className="sc-narrow-container text-center">
      <Link className="sc-orange-button sc-start" to="/sessions/new">
        Start using SonarCloud <ChevronRightIcon className="spacer-left" />
      </Link>
      <div className="big-spacer-top">
        <a
          className="text-muted"
          href="https://community.sonarsource.com/c/help/sc"
          rel="noopener noreferrer"
          target="_blank">
          Need help?
        </a>
      </div>
    </div>
  );
}
