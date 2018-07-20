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
import * as React from 'react';
import * as classNames from 'classnames';
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import AutoProjectCreate from './AutoProjectCreate';
import ManualProjectCreate from './ManualProjectCreate';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import { getCurrentUser } from '../../../store/rootReducer';
import { skipOnboarding } from '../../../store/users/actions';
import { CurrentUser, isLoggedIn } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import { ProjectBase } from '../../../api/components';
import { getProjectUrl, getOrganizationUrl } from '../../../helpers/urls';
import '../../../app/styles/sonarcloud.css';
import '../styles.css';

interface OwnProps {
  onFinishOnboarding: () => void;
}

interface StateProps {
  currentUser: CurrentUser;
}

interface DispatchProps {
  skipOnboarding: () => void;
}

enum Tabs {
  AUTO,
  MANUAL
}

type Props = OwnProps & StateProps & DispatchProps;

interface State {
  activeTab: Tabs;
}

export class CreateProjectOnboarding extends React.PureComponent<Props, State> {
  mounted = false;
  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = { activeTab: this.shouldDisplayTabs(props) ? Tabs.AUTO : Tabs.MANUAL };
  }

  componentDidMount() {
    this.mounted = true;
    if (!isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
    }
    document.body.classList.add('white-page');
    document.documentElement.classList.add('white-page');
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    document.documentElement.classList.remove('white-page');
  }

  handleProjectCreate = (projects: Pick<ProjectBase, 'key'>[], organization?: string) => {
    if (projects.length > 1 && organization) {
      this.context.router.push(getOrganizationUrl(organization) + '/projects');
    } else if (projects.length === 1) {
      this.context.router.push(getProjectUrl(projects[0].key));
    }
  };

  shouldDisplayTabs = ({ currentUser } = this.props) => {
    return (
      isLoggedIn(currentUser) &&
      ['bitbucket', 'github'].includes(currentUser.externalProvider || '')
    );
  };

  showAuto = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ activeTab: Tabs.AUTO });
  };

  showManual = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ activeTab: Tabs.MANUAL });
  };

  render() {
    const { currentUser } = this.props;
    if (!isLoggedIn(currentUser)) {
      return null;
    }

    const { activeTab } = this.state;
    const header = translate('onboarding.create_project.header');
    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="sonarcloud page page-limited">
          <div className="page-header">
            <h1 className="page-title">{header}</h1>
          </div>

          {this.shouldDisplayTabs() && (
            <ul className="flex-tabs">
              <li>
                <a
                  className={classNames('js-auto', { selected: activeTab === Tabs.AUTO })}
                  href="#"
                  onClick={this.showAuto}>
                  {translate('onboarding.create_project.select_repositories')}
                  <span
                    className={classNames(
                      'rounded alert alert-small spacer-left display-inline-block',
                      {
                        'alert-info': activeTab === Tabs.AUTO,
                        'alert-muted': activeTab !== Tabs.AUTO
                      }
                    )}>
                    {translate('beta')}
                  </span>
                </a>
              </li>
              <li>
                <a
                  className={classNames('js-manual', { selected: activeTab === Tabs.MANUAL })}
                  href="#"
                  onClick={this.showManual}>
                  {translate('onboarding.create_project.create_manually')}
                </a>
              </li>
            </ul>
          )}

          {activeTab === Tabs.AUTO ? (
            <AutoProjectCreate currentUser={currentUser} />
          ) : (
            <ManualProjectCreate
              currentUser={currentUser}
              onProjectCreate={this.handleProjectCreate}
            />
          )}
        </div>
      </>
    );
  }
}

const mapStateToProps = (state: any): StateProps => {
  return {
    currentUser: getCurrentUser(state)
  };
};

const mapDispatchToProps: DispatchProps = { skipOnboarding };

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  CreateProjectOnboarding
);
