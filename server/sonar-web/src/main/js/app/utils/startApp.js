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
import _ from 'underscore';
import Navigation from '../components/nav/app';
import { installGlobal, requestMessages } from '../../helpers/l10n';

function requestLocalizationBundle () {
  if (!window.sonarqube.bannedNavigation) {
    installGlobal();
    return requestMessages();
  } else {
    return Promise.resolve();
  }
}

function startNavigation () {
  if (!window.sonarqube.bannedNavigation) {
    return new Navigation().start();
  } else {
    return Promise.resolve();
  }
}

function prepareAppOptions (navResponse) {
  const appOptions = { el: '#content' };
  if (navResponse) {
    appOptions.rootQualifiers = navResponse.global.qualifiers;
    appOptions.logoUrl = navResponse.global.logoUrl;
    appOptions.logoWidth = navResponse.global.logoWidth;
    if (navResponse.component) {
      appOptions.component = {
        id: navResponse.component.uuid,
        key: navResponse.component.key,
        name: navResponse.component.name,
        qualifier: _.last(navResponse.component.breadcrumbs).qualifier,
        breadcrumbs: navResponse.component.breadcrumbs,
        snapshotDate: navResponse.component.snapshotDate
      };
    }
  }
  return appOptions;
}

const startApp = () => {
  window.sonarqube.appStarted = Promise.resolve()
      .then(requestLocalizationBundle)
      .then(startNavigation)
      .then(prepareAppOptions);
};

export default startApp;

