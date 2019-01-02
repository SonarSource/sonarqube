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

export default function SQTopNav() {
  return (
    <ul className="sc-top-nav">
      <li className="sc-top-nav-item">
        <Link
          activeClassName="sc-top-nav-active"
          className="sc-top-nav-link"
          to="/about/sq/as-a-service">
          As a Service
        </Link>
      </li>
      <li className="sc-top-nav-item">
        <Link
          activeClassName="sc-top-nav-active"
          className="sc-top-nav-link"
          to="/about/sq/branch-analysis-and-pr-decoration">
          Branch analysis & PR decoration
        </Link>
      </li>
      <li className="sc-top-nav-item">
        <Link
          activeClassName="sc-top-nav-active"
          className="sc-top-nav-link"
          to="/about/sq/sonarlint-integration">
          SonarLint integration
        </Link>
      </li>
    </ul>
  );
}
