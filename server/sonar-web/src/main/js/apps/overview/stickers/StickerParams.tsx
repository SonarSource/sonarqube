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
import { connect } from 'react-redux';
import Select from '../../../components/controls/Select';
import { fetchWebApi } from '../../../api/web-api';
import { Metric } from '../../../app/types';
import { StickerColors, StickerType, StickerOptions } from './utils';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics } from '../../../store/rootReducer';

interface StateToProps {
  metrics: { [key: string]: Metric };
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

interface OwnProps {
  className?: string;
  options: StickerOptions;
  type: StickerType;
  updateOptions: (options: Partial<StickerOptions>) => void;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  stickerMetrics: string[];
}

export class StickerParams extends React.PureComponent<Props> {
  mounted: boolean;
  state: State = { stickerMetrics: [] };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchMetrics();
    this.fetchStickerMetrics();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchStickerMetrics() {
    fetchWebApi(false).then(
      webservices => {
        if (this.mounted) {
          const domain = webservices.find(domain => domain.path === 'api/stickers');
          const ws = domain && domain.actions.find(ws => ws.key === 'measure');
          const param = ws && ws.params && ws.params.find(param => param.key === 'metric');
          if (param && param.possibleValues) {
            this.setState({ stickerMetrics: param.possibleValues });
          }
        }
      },
      () => {}
    );
  }

  getColorOptions = () =>
    ['white', 'black', 'orange'].map(color => ({
      label: translate('overview.stickers.options.colors', color),
      value: color
    }));

  getMetricOptions = () => {
    const { metrics } = this.props;
    return this.state.stickerMetrics.map(key => {
      const metric = metrics[key];
      return {
        value: key,
        label: metric && getLocalizedMetricName(metric)
      };
    });
  };

  handleColorChange = ({ value }: { value: StickerColors }) =>
    this.props.updateOptions({ color: value });

  handleMetricChange = ({ value }: { value: string }) =>
    this.props.updateOptions({ metric: value });

  render() {
    const { className, options, type } = this.props;
    switch (type) {
      case StickerType.marketing:
        return (
          <div className={className}>
            <label className="big-spacer-right" htmlFor="sticker-color">
              {translate('color')}
            </label>
            <Select
              className="input-medium"
              clearable={false}
              name="sticker-color"
              onChange={this.handleColorChange}
              options={this.getColorOptions()}
              searchable={false}
              value={options.color}
            />
          </div>
        );
      case StickerType.measure:
        return (
          <div className={className}>
            <label className="big-spacer-right" htmlFor="sticker-metric">
              {translate('overview.stickers.metric')}
            </label>
            <Select
              className="input-medium"
              clearable={false}
              name="sticker-metric"
              onChange={this.handleMetricChange}
              options={this.getMetricOptions()}
              searchable={false}
              value={options.metric}
            />
          </div>
        );
      default:
        return null;
    }
  }
}

const mapDispatchToProps: DispatchToProps = { fetchMetrics };

const mapStateToProps = (state: any): StateToProps => ({
  metrics: getMetrics(state)
});

export default connect<StateToProps, DispatchToProps, OwnProps>(
  mapStateToProps,
  mapDispatchToProps
)(StickerParams);
