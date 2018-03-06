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
import { parse } from 'querystring';
import React from 'react';
import { render } from 'react-dom';
import Configuration from './components/Configuration';
import Widget from './components/Widget';
import './vsts.css';

VSS.init({
  explicitNotifyLoaded: true,
  usePlatformStyles: true
});

VSS.require('TFS/Dashboards/WidgetHelpers', widgetHelpers => {
  const container = document.getElementById('content');
  const query = parse(window.location.search.replace('?', ''));

  if (query.type === 'configuration') {
    render(<Configuration widgetHelpers={widgetHelpers} />, container);
  } else {
    render(<Widget widgetHelpers={widgetHelpers} />, container);
  }
  VSS.notifyLoadSucceeded();
});
