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
import { AppState } from '../../types/appstate';
import { GlobalSettingKeys } from '../../types/settings';
import withAppStateContext from './app-state/withAppStateContext';
import KeyboardShortcutsModal from './KeyboardShortcutsModal';
import PageTracker from './PageTracker';

interface Props {
  appState: AppState;
}

export class App extends React.PureComponent<Props> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;
    this.setScrollbarWidth();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  setScrollbarWidth = () => {
    // See https://stackoverflow.com/questions/13382516/getting-scroll-bar-width-using-javascript
    const outer = document.createElement('div');
    outer.style.visibility = 'hidden';
    outer.style.width = '100px';
    outer.style.setProperty('msOverflowStyle', 'scrollbar');

    document.body.appendChild(outer);

    const widthNoScroll = outer.offsetWidth;
    outer.style.overflow = 'scroll';

    const inner = document.createElement('div');
    inner.style.width = '100%';
    outer.appendChild(inner);

    const widthWithScroll = inner.offsetWidth;

    if (outer.parentNode) {
      outer.parentNode.removeChild(outer);
    }

    document.body.style.setProperty('--sbw', `${widthNoScroll - widthWithScroll}px`);
  };

  renderPreconnectLink = () => {
    const {
      appState: { settings },
    } = this.props;

    const enableGravatar = settings[GlobalSettingKeys.EnableGravatar] === 'true';
    const gravatarServerUrl = settings[GlobalSettingKeys.GravatarServerUrl];

    if (!enableGravatar || !gravatarServerUrl) {
      return null;
    }

    const parser = document.createElement('a');
    parser.href = gravatarServerUrl;
    if (parser.hostname !== window.location.hostname) {
      return <link href={parser.origin} rel="preconnect" />;
    }
    return null;
  };

  render() {
    return (
      <>
        <PageTracker>{this.renderPreconnectLink()}</PageTracker>
        <Outlet />
        <KeyboardShortcutsModal />
      </>
    );
  }
}

export default withAppStateContext(App);
