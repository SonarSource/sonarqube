/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { WithRouterProps } from 'react-router';
import { BitbucketProjectRepositories, BitbucketRepository } from '../../../types/alm-integration';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import BitbucketCloudProjectCreateRenderer from './BitbucketCloudProjectCreateRender';

interface Props extends Pick<WithRouterProps, 'location' | 'router'> {
  canAdmin: boolean;
  settings: AlmSettingsInstance[];
  loadingBindings: boolean;
  onProjectCreate: (projectKeys: string[]) => void;
}

interface State {
  settings: AlmSettingsInstance;
  loading: boolean;
  projectRepositories?: BitbucketProjectRepositories;
  searchResults?: BitbucketRepository[];
  selectedRepository?: BitbucketRepository;
  showPersonalAccessTokenForm: boolean;
}

export default class BitbucketCloudProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      // For now, we only handle a single instance. So we always use the first
      // one from the list.
      settings: props.settings[0],
      loading: false,
      showPersonalAccessTokenForm: true
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.settings.length === 0 && this.props.settings.length > 0) {
      this.setState({ settings: this.props.settings[0] }, () => this.fetchData());
    }
  }

  handlePersonalAccessTokenCreated = async () => {
    this.setState({ showPersonalAccessTokenForm: false });
    this.cleanUrl();
    await this.fetchData();
  };

  cleanUrl = () => {
    const { location, router } = this.props;
    delete location.query.resetPat;
    router.replace(location);
  };

  async fetchData() {}

  render() {
    const { canAdmin, loadingBindings, location } = this.props;
    const { settings, loading, showPersonalAccessTokenForm } = this.state;
    return (
      <BitbucketCloudProjectCreateRenderer
        settings={settings}
        canAdmin={canAdmin}
        loading={loading || loadingBindings}
        onPersonalAccessTokenCreated={this.handlePersonalAccessTokenCreated}
        resetPat={Boolean(location.query.resetPat)}
        showPersonalAccessTokenForm={
          showPersonalAccessTokenForm || Boolean(location.query.resetPat)
        }
      />
    );
  }
}
