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
import { installPlugin, uninstallPlugin, updatePlugin } from '../../../api/plugins';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import Checkbox from '../../../components/controls/Checkbox';
import Tooltip from '../../../components/controls/Tooltip';
import CheckIcon from '../../../components/icons/CheckIcon';
import { translate } from '../../../helpers/l10n';
import { isAvailablePlugin, isInstalledPlugin, Plugin } from '../../../types/plugins';
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
      }
    );
  };

  handleInstall = () => this.doPluginAction(installPlugin);
  handleUpdate = () => this.doPluginAction(updatePlugin);
  handleUninstall = () => this.doPluginAction(uninstallPlugin);
  handleTermsCheck = (checked: boolean) => this.setState({ acceptTerms: checked });

  renderBundled() {
    const { plugin } = this.props;

    return (
      <div className="js-actions">
        {isAvailablePlugin(plugin) && (
          <div>
            <p className="little-spacer-bottom">
              {translate('marketplace.available_under_commercial_license')}
            </p>
            {plugin.homepageUrl && (
              <Link to={plugin.homepageUrl} target="_blank">
                {translate('marketplace.learn_more')}
              </Link>
            )}
          </div>
        )}
        {isInstalledPlugin(plugin) && (
          <p>
            <CheckIcon className="little-spacer-right" />
            {translate('marketplace.installed')}
          </p>
        )}
        {isInstalledPlugin(plugin) && plugin.updates && plugin.updates.length > 0 && (
          <div className="spacer-top">
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
      <div className="js-actions">
        {isAvailablePlugin(plugin) && plugin.termsAndConditionsUrl && (
          <p className="little-spacer-bottom">
            <Checkbox
              checked={this.state.acceptTerms}
              className="js-terms"
              id={'plugin-terms-' + plugin.key}
              onCheck={this.handleTermsCheck}
            >
              <label className="little-spacer-left" htmlFor={'plugin-terms-' + plugin.key}>
                {translate('marketplace.i_accept_the')}
              </label>
            </Checkbox>
            <a
              className="js-plugin-terms nowrap little-spacer-left"
              href={plugin.termsAndConditionsUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              {translate('marketplace.terms_and_conditions')}
            </a>
          </p>
        )}
        {loading && <i className="spinner spacer-right little-spacer-top little-spacer-bottom" />}
        {isInstalledPlugin(plugin) && (
          <>
            {plugin.updates &&
              plugin.updates.map((update, idx) => (
                <PluginUpdateButton
                  disabled={loading}
                  key={idx}
                  onClick={this.handleUpdate}
                  update={update}
                />
              ))}
            <Tooltip overlay={translate('marketplace.requires_restart')}>
              <Button
                className="js-uninstall button-red little-spacer-left"
                disabled={loading}
                onClick={this.handleUninstall}
              >
                {translate('marketplace.uninstall')}
              </Button>
            </Tooltip>
          </>
        )}
        {isAvailablePlugin(plugin) && (
          <Tooltip overlay={translate('marketplace.requires_restart')}>
            <Button
              className="js-install"
              disabled={
                loading || (plugin.termsAndConditionsUrl != null && !this.state.acceptTerms)
              }
              onClick={this.handleInstall}
            >
              {translate('marketplace.install')}
            </Button>
          </Tooltip>
        )}
      </div>
    );
  }
}
