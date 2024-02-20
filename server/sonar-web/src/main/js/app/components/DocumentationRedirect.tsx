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
import { Link } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { useLocation } from 'react-router-dom';
import { useDocUrl } from '../../helpers/docs';

const PAUSE_REDIRECT = 1;

export default function DocumentationRedirect() {
  const location = useLocation();
  const url = useDocUrl(location.pathname.replace(/^\/documentation/, ''));

  return (
    <>
      <Helmet>
        <meta httpEquiv="refresh" content={`${PAUSE_REDIRECT}; url='${url}'`} />
      </Helmet>
      <div className="sw-flex sw-flex-col sw-items-center sw-gap-4 sw-h-[100vh]">
        <div className="global-loading">
          <i className="global-loading-spinner" />
          <span className="global-loading-text">Redirecting...</span>
        </div>
        <div>
          <Link to={url}>Click here if you&apos;re not being redirected automatically</Link>
        </div>
      </div>
    </>
  );
}
