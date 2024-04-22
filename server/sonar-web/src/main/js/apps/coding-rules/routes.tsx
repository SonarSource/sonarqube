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
import React, { useEffect } from 'react';
import { Route, useLocation, useNavigate } from 'react-router-dom';
import { RawQuery } from '~sonar-aligned/types/router';
import CodingRulesApp from './components/CodingRulesApp';
import { parseQuery, serializeQuery } from './query';

const EXPECTED_SPLIT_PARTS = 2;

function parseHash(hash: string): RawQuery {
  const query: RawQuery = {};
  const parts = hash.split('|');
  parts.forEach((part) => {
    const tokens = part.split('=');
    if (tokens.length === EXPECTED_SPLIT_PARTS) {
      query[decodeURIComponent(tokens[0])] = decodeURIComponent(tokens[1]);
    }
  });
  return query;
}

function HashEditWrapper() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const { hash } = location;
    if (hash.length > 1) {
      const query = parseHash(hash.substring(1));
      const normalizedQuery = {
        ...serializeQuery(parseQuery(query)),
        open: query.open,
      };
      navigate(
        { pathname: location.pathname, search: new URLSearchParams(normalizedQuery).toString() },
        { replace: true },
      );
    }
  }, [location, navigate]);

  return <CodingRulesApp />;
}

const routes = () => <Route path="coding_rules" element={<HashEditWrapper />} />;

export default routes;
