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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { ButtonSecondary, CheckIcon, Checkbox, Link, Spinner } from 'design-system';
import * as React from 'react';
import { installPlugin, uninstallPlugin, updatePlugin } from '../../../api/plugins';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { Plugin, isAvailablePlugin, isInstalledPlugin } from '../../../types/plugins';
import PluginUpdateButton from './PluginUpdateButton';

interface Props {
  plugin: Plugin;
  refreshPending: () => void;
}

interface State {
  acceptTerms: boolean;
  loading: boolean;
}

export default class PluginActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { acceptTerms: false, loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  doPluginAction = (apiAction: (data: { key: string }) => Promise<void | Response>) => {
    this.setState({ loading: true });
    apiAction({ key: this.props.plugin.key }).then(
      () => {
        this.props.refreshPending();
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  handleInstall = () => this.doPluginAction(installPlugin);
  handleUpdate = () => this.doPluginAction(updatePlugin);
  handleUninstall = () => this.doPluginAction(uninstallPlugin);
  handleTermsCheck = (checked: boolean) => this.setState({ acceptTerms: checked });

  renderBundled() {
    const { plugin } = this.props;

    return (
      <div className="it__js-actions">
        {isAvailablePlugin(plugin) && (
          <div>
            <p className="sw-mb-1">{translate('marketplace.available_under_commercial_license')}</p>
            {plugin.homepageUrl && (
              <Link to={plugin.homepageUrl} target="_blank">
                {translate('marketplace.learn_more')}
              </Link>
            )}
          </div>
        )}
        {isInstalledPlugin(plugin) && (
          <p>
            <CheckIcon className="sw-mr-1" />
            {translate('marketplace.installed')}
          </p>
        )}
        {isInstalledPlugin(plugin) && plugin.updates && plugin.updates.length > 0 && (
          <div className="sw-mt-2">
            {plugin.updates.map((update, idx) => (
              <PluginUpdateButton
                disabled={this.state.loading}
                key={idx}
                onClick={this.handleUpdate}
                update={update}
              />
            ))}
          </div>
        )}
      </div>
    );
  }

  render() {
    const { plugin } = this.props;

    if (plugin.editionBundled) {
      return this.renderBundled();
    }

    const { loading } = this.state;
    return (
      <div className="it__js-actions">
        {isAvailablePlugin(plugin) && plugin.termsAndConditionsUrl && (
          <div className="sw-flex sw-items-center sw-flex-wrap sw-mb-2">
            <Checkbox
              checked={this.state.acceptTerms}
              id={'plugin-terms-' + plugin.key}
              onCheck={this.handleTermsCheck}
            >
              <span className="sw-ml-2">{translate('marketplace.i_accept_the')}</span>
            </Checkbox>
            <Link className="sw-whitespace-nowrap sw-ml-1" to={plugin.termsAndConditionsUrl}>
              {translate('marketplace.terms_and_conditions')}
            </Link>
          </div>
        )}
        <Spinner className="sw-my-2" loading={loading} />
        {isInstalledPlugin(plugin) && (
          <>
            {plugin.updates?.map((update, idx) => (
              <div className="sw-inline-block sw-mr-2 sw-mb-2" key={idx}>
                <PluginUpdateButton
                  disabled={loading}
                  onClick={this.handleUpdate}
                  update={update}
                />
              </div>
            ))}
            <Tooltip content={translate('marketplace.requires_restart')}>
              <Button
                isDisabled={loading}
                onClick={this.handleUninstall}
                variety={ButtonVariety.DangerOutline}
              >
                {translate('marketplace.uninstall')}
              </Button>
            </Tooltip>
          </>
        )}
        {isAvailablePlugin(plugin) && (
          <Tooltip content={translate('marketplace.requires_restart')}>
            <ButtonSecondary
              disabled={
                loading || (plugin.termsAndConditionsUrl != null && !this.state.acceptTerms)
              }
              onClick={this.handleInstall}
            >
              {translate('marketplace.install')}
            </ButtonSecondary>
          </Tooltip>
        )}
      </div>
    );
  }
}
