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
import * as React from 'react';
import { connect } from 'react-redux';
import { lazyLoad } from 'sonar-ui-common/components/lazyLoad';
import { fetchMyOrganizations } from '../../apps/account/organizations/actions';
import { isSonarCloud } from '../../helpers/system';
import { isLoggedIn } from '../../helpers/users';
import { fetchLanguages } from '../../store/rootActions';
import { getAppState, getCurrentUser, getGlobalSettingValue, Store } from '../../store/rootReducer';

const PageTracker = lazyLoad(() => import('./PageTracker'));

interface StateProps {
  appState: T.AppState | undefined;
  currentUser: T.CurrentUser | undefined;
  enableGravatar: boolean;
  gravatarServerUrl: string;
}

interface DispatchProps {
  fetchLanguages: () => Promise<void>;
  fetchMyOrganizations: () => Promise<void>;
}

interface OwnProps {
  children: JSX.Element;
}

type Props = StateProps & DispatchProps & OwnProps;

class App extends React.PureComponent<Props> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;
    this.props.fetchLanguages();
    this.setScrollbarWidth();
    const { appState, currentUser } = this.props;
    if (appState && isSonarCloud() && currentUser && isLoggedIn(currentUser)) {
      this.props.fetchMyOrganizations();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  setScrollbarWidth = () => {
    // See https://stackoverflow.com/questions/13382516/getting-scroll-bar-width-using-javascript
    const outer = document.createElement('div');
    outer.style.visibility = 'hidden';
    outer.style.width = '100px';
    outer.style.msOverflowStyle = 'scrollbar';

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
    } else {
      return null;
    }
  };

  render() {
    return (
      <>
        <PageTracker>{this.props.enableGravatar && this.renderPreconnectLink()}</PageTracker>
        {this.props.children}
      </>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => {
  const enableGravatar = getGlobalSettingValue(state, 'sonar.lf.enableGravatar');
  const gravatarServerUrl = getGlobalSettingValue(state, 'sonar.lf.gravatarServerUrl');
  return {
    appState: getAppState(state),
    currentUser: getCurrentUser(state),
    enableGravatar: Boolean(enableGravatar && enableGravatar.value === 'true'),
    gravatarServerUrl: (gravatarServerUrl && gravatarServerUrl.value) || ''
  };
};

const mapDispatchToProps = ({
  fetchLanguages,
  fetchMyOrganizations
} as any) as DispatchProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
