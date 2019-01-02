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
import { parse } from 'querystring';
import React from 'react';
import { render } from 'react-dom';
import Configuration from './components/Configuration';
import Widget from './components/Widget';
import { parseWidgetSettings } from './utils';
import './vsts.css';

const container = document.getElementById('content');
const query = parse(window.location.search.replace('?', ''));

if (query.type === 'authenticated') {
  if (window.opener && window.opener.authenticationDone) {
    window.opener.authenticationDone();
  }
  window.close();
} else if (VSS && query.contribution && VSS.init && VSS.require) {
  VSS.init({
    explicitNotifyLoaded: true,
    usePlatformStyles: true
  });

  VSS.require('TFS/Dashboards/WidgetHelpers', WidgetHelpers => {
    WidgetHelpers.IncludeWidgetStyles();
    WidgetHelpers.IncludeWidgetConfigurationStyles();

    if (query.type === 'configuration') {
      render(
        <Configuration contribution={query.contribution} widgetHelpers={WidgetHelpers} />,
        container
      );
    } else {
      VSS.register(query.contribution, () => {
        const loadFunction = loadVSTSWidget(WidgetHelpers);
        return { load: loadFunction, reload: loadFunction };
      });
    }
    VSS.notifyLoadSucceeded();
  });
}

function loadVSTSWidget(WidgetHelpers) {
  return widgetSettings => {
    try {
      render(<Widget settings={parseWidgetSettings(widgetSettings)} />, container);
    } catch (error) {
      return WidgetHelpers.WidgetStatusHelper.Failure(error);
    }

    return WidgetHelpers.WidgetStatusHelper.Success();
  };
}
