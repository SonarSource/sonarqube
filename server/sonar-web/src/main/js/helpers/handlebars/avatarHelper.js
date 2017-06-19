/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
const React = require('react');
const { renderToString } = require('react-dom/server');
const Handlebars = require('handlebars/runtime');
const WithStore = require('../../components/shared/WithStore').default;
const Avatar = require('../../components/ui/Avatar').default;

module.exports = function(email, name, size) {
  return new Handlebars.default.SafeString(
    renderToString(
      <WithStore>
        <Avatar email={email} name={name} size={size} />
      </WithStore>
    )
  );
};
