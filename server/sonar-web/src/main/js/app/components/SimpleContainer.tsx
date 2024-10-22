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

import * as React from 'react';
import { Outlet } from 'react-router-dom';
import GlobalFooter from './GlobalFooter';
import MainSonarQubeBar from './nav/global/MainSonarQubeBar';

/*
 * We need to render either children or the Outlet,
 * because this component is used both in the context of routes and as a regular container
 */
export default function SimpleContainer({ children }: { children?: React.ReactNode }) {
  return (
    <div className="sw-flex sw-flex-col sw-h-full sw-min-h-[100vh]">
      <div className="sw-box-border sw-flex-auto" id="container">
        <MainSonarQubeBar />
        {children !== undefined ? children : <Outlet />}
      </div>
      <GlobalFooter />
    </div>
  );
}
