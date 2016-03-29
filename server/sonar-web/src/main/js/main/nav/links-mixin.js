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
/* eslint no-unused-vars: 0 */
import _ from 'underscore';
import React from 'react';

export default {
  activeLink(url) {
    return window.location.pathname.indexOf(window.baseUrl + url) === 0 ? 'active' : null;
  },

  renderLink(url, title, highlightUrl = url) {
    let fullUrl = window.baseUrl + url;
    let check = _.isFunction(highlightUrl) ? highlightUrl : this.activeLink;
    return (
        <li key={url} className={check(highlightUrl)}>
          <a href={fullUrl}>{title}</a>
        </li>
    );
  },

  renderNewLink(url, title, highlightUrl = url) {
    let fullUrl = window.baseUrl + url;
    let check = _.isFunction(highlightUrl) ? highlightUrl : this.activeLink;
    return (
        <li key={highlightUrl} className={check(highlightUrl)}>
          <a href={fullUrl} className="nowrap">{title} <span className="spacer-left badge">New</span></a>
        </li>
    );
  }
};
