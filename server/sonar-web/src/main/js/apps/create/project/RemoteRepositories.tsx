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
import * as classNames from 'classnames';
import AlmRepositoryItem from './AlmRepositoryItem';
import SetupProjectBox from './SetupProjectBox';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import SearchBox from '../../../components/controls/SearchBox';
import UpgradeOrganizationBox from '../components/UpgradeOrganizationBox';
import { Alert } from '../../../components/ui/Alert';
import { getRepositories } from '../../../api/alm-integration';
import { isDefined } from '../../../helpers/types';
import { translateWithParameters, translate } from '../../../helpers/l10n';

interface Props {
  almApplication: T.AlmApplication;
  onOrganizationUpgrade: () => void;
  onProjectCreate: (projectKeys: string[], organization: string) => void;
  organization: T.Organization;
}

type SelectedRepositories = T.Dict<T.AlmRepository | undefined>;

interface State {
  highlight: boolean;
  loading: boolean;
  repositories: T.AlmRepository[];
  search: string;
  selectedRepositories: SelectedRepositories;
  successfullyUpgraded: boolean;
}

export default class RemoteRepositories extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    highlight: false,
    loading: true,
    repositories: [],
    search: '',
    selectedRepositories: {},
    successfullyUpgraded: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchRepositories();
  }

  componentDidUpdate(prevProps: Props) {
    const { organization } = this.props;
    if (prevProps.organization !== organization) {
      this.setState({ loading: true, selectedRepositories: {} });
      this.fetchRepositories();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRepositories = () => {
    const { organization } = this.props;
    return getRepositories({ organization: organization.key }).then(
      ({ repositories }) => {
        if (this.mounted) {
          this.setState({ loading: false, repositories });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleHighlightUpgradeBox = (highlight: boolean) => {
    this.setState({ highlight });
  };

  handleOrganizationUpgrade = () => {
    this.props.onOrganizationUpgrade();
    if (this.mounted) {
      this.setState({ successfullyUpgraded: true });
    }
  };

  handleProvisionFail = () => {
    return this.fetchRepositories().then(() => {
      if (this.mounted) {
        this.setState(({ repositories, selectedRepositories }) => {
          const updateSelectedRepositories: SelectedRepositories = {};
          Object.keys(selectedRepositories).forEach(installationKey => {
            const newRepository = repositories.find(r => r.installationKey === installationKey);
            if (newRepository && !newRepository.linkedProjectKey) {
              updateSelectedRepositories[newRepository.installationKey] = newRepository;
            }
          });
          return { selectedRepositories: updateSelectedRepositories };
        });
      }
    });
  };

  handleSearch = (search: string) => {
    this.setState({ search });
  };

  toggleRepository = (repository: T.AlmRepository) => {
    this.setState(({ selectedRepositories }) => ({
      selectedRepositories: {
        ...selectedRepositories,
        [repository.installationKey]: selectedRepositories[repository.installationKey]
          ? undefined
          : repository
      }
    }));
  };

  render() {
    const { highlight, loading, repositories, search, selectedRepositories } = this.state;
    const { almApplication, organization } = this.props;
    const isPaidOrg = organization.subscription === 'PAID';
    const hasPrivateRepositories = repositories.some(repository => Boolean(repository.private));
    const showSearchBox = repositories.length > 5;
    const showUpgradebox =
      !isPaidOrg && hasPrivateRepositories && organization.actions && organization.actions.admin;
    const filteredRepositories = repositories.filter(
      repo => !search || repo.label.toLowerCase().includes(search.toLowerCase())
    );
    return (
      <div className="create-project">
        <div className="flex-1 huge-spacer-right">
          {showSearchBox && (
            <div className="spacer-bottom">
              <SearchBox
                minLength={1}
                onChange={this.handleSearch}
                placeholder={translate('search.search_for_repositories')}
                value={this.state.search}
              />
            </div>
          )}
          {this.state.successfullyUpgraded && (
            <Alert variant="success">
              {translateWithParameters(
                'onboarding.create_project.subscribtion_success_x',
                organization.name
              )}
            </Alert>
          )}
          <DeferredSpinner loading={loading}>
            <ul>
              {filteredRepositories.length === 0 && (
                <li className="big-spacer-top note">
                  {showUpgradebox
                    ? translateWithParameters('no_results_for_x', search)
                    : translate('onboarding.create_project.no_repositories')}
                </li>
              )}
              {filteredRepositories.map(repo => (
                <AlmRepositoryItem
                  disabled={Boolean(repo.private && !isPaidOrg)}
                  highlightUpgradeBox={this.handleHighlightUpgradeBox}
                  identityProvider={almApplication}
                  key={repo.installationKey}
                  repository={repo}
                  selected={Boolean(selectedRepositories[repo.installationKey])}
                  toggleRepository={this.toggleRepository}
                />
              ))}
            </ul>
          </DeferredSpinner>
        </div>
        {organization && (
          <div className={classNames({ 'create-project-side-with-search': showSearchBox })}>
            <div className="create-project-side-sticky">
              <SetupProjectBox
                onProjectCreate={this.props.onProjectCreate}
                onProvisionFail={this.handleProvisionFail}
                organization={organization}
                selectedRepositories={Object.keys(selectedRepositories)
                  .map(r => selectedRepositories[r])
                  .filter(isDefined)}
              />
              {showUpgradebox && (
                <UpgradeOrganizationBox
                  className={classNames({ highlight })}
                  onOrganizationUpgrade={this.handleOrganizationUpgrade}
                  organization={organization}
                />
              )}
            </div>
          </div>
        )}
      </div>
    );
  }
}
