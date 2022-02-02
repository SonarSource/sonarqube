/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { connect } from 'react-redux';
import { lazyLoadComponent } from '../../components/lazyLoadComponent';
import { getGlobalSettingValue, Store } from '../../store/rootReducer';
import KeyboardShortcutsModal from './KeyboardShortcutsModal';

const PageTracker = lazyLoadComponent(() => import('./PageTracker'));

interface Props {
  enableGravatar: boolean;
  gravatarServerUrl: string;
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
    const parser = document.createElement('a');
    parser.href = this.props.gravatarServerUrl;
    if (parser.hostname !== window.location.hostname) {
      return <link href={parser.origin} rel="preconnect" />;
    }
    return null;
  };

  render() {
    return (
      <>
        <PageTracker>{this.props.enableGravatar && this.renderPreconnectLink()}</PageTracker>
        {this.props.children}
        <KeyboardShortcutsModal />
      </>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const enableGravatar = getGlobalSettingValue(state, 'sonar.lf.enableGravatar');
  const gravatarServerUrl = getGlobalSettingValue(state, 'sonar.lf.gravatarServerUrl');
  return {
    enableGravatar: Boolean(enableGravatar && enableGravatar.value === 'true'),
    gravatarServerUrl: (gravatarServerUrl && gravatarServerUrl.value) || ''
  };
};

export default connect(mapStateToProps)(App);
