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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getDefinitions, getValues, setSimpleSettingValue } from '../../api/settings';
import { SettingCategoryDefinition, SettingsKey } from '../../types/settings';
import ReportFrequencyForm from './ReportFrequencyForm';

interface Props {
  component: T.Component;
}

interface State {
  definition?: SettingCategoryDefinition;
  frequency?: string;
  loading: boolean;
}

export default class ApplicationSettingsApp extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchInitialData();
  }

  async componentDidUpdate(prevProps: Props) {
    if (prevProps.component.key !== this.props.component.key) {
      this.setState({ loading: true });
      await this.fetchSetting();
      this.setState({ loading: false });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchInitialData = async () => {
    this.setState({ loading: true });
    await Promise.all([this.fetchDefinition(), this.fetchSetting()]);
    this.setState({ loading: false });
  };

  fetchDefinition = async () => {
    const { component } = this.props;
    try {
      const definitions = await getDefinitions(component.key);
      const definition = definitions.find(d => d.key === SettingsKey.ProjectReportFrequency);

      if (this.mounted) {
        this.setState({ definition });
      }
    } catch (_) {
      /* do nothing */
    }
  };

  fetchSetting = async () => {
    const { component } = this.props;
    try {
      const setting = (
        await getValues({ component: component.key, keys: SettingsKey.ProjectReportFrequency })
      ).pop();

      if (this.mounted) {
        this.setState({
          frequency: setting ? setting.value : undefined
        });
      }
    } catch (_) {
      /* do nothing */
    }
  };

  handleSubmit = async (frequency: string) => {
    const { component } = this.props;

    try {
      await setSimpleSettingValue({
        component: component.key,
        key: SettingsKey.ProjectReportFrequency,
        value: frequency
      });

      this.setState({ frequency });
    } catch (_) {
      /* Do nothing */
    }
  };

  render() {
    const { definition, frequency, loading } = this.state;

    return (
      <div className="page page-limited application-settings">
        <h1>{translate('application_settings.page')}</h1>
        <div className="boxed-group big-padded big-spacer-top">
          <DeferredSpinner loading={loading}>
            <div>
              {definition && frequency && (
                <ReportFrequencyForm
                  definition={definition}
                  frequency={frequency}
                  onSave={this.handleSubmit}
                />
              )}
            </div>
          </DeferredSpinner>
        </div>
      </div>
    );
  }
}
