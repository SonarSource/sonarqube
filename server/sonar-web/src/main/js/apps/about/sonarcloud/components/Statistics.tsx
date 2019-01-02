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
import { throttle } from 'lodash';
import CountUp from 'react-countup';
import { formatMeasure } from '../../../../helpers/measures';
import { getBaseUrl } from '../../../../helpers/urls';
import './Statistics.css';

interface Statistic {
  icon: string;
  text: string;
  value: number;
}

interface Props {
  statistics: Statistic[];
}

export default function Statistics({ statistics }: Props) {
  return (
    <div className="sc-stats">
      {statistics.map(stat => (
        <StatisticCard key={stat.icon} statistic={stat} />
      ))}
    </div>
  );
}

interface StatisticCardProps {
  statistic: Statistic;
}

interface StatisticCardState {
  viewable: boolean;
}

export class StatisticCard extends React.PureComponent<StatisticCardProps, StatisticCardState> {
  container?: HTMLElement | null;

  constructor(props: StatisticCardProps) {
    super(props);
    this.state = { viewable: false };
    this.handleScroll = throttle(this.handleScroll, 10);
  }

  componentDidMount() {
    document.addEventListener('scroll', this.handleScroll, true);
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.handleScroll, true);
  }

  handleScroll = () => {
    if (this.container) {
      const rect = this.container.getBoundingClientRect();
      const windowHeight =
        window.innerHeight ||
        (document.documentElement ? document.documentElement.clientHeight : 0);
      if (rect.top <= windowHeight && rect.top + rect.height >= 0) {
        this.setState({ viewable: true });
      }
    }
  };

  render() {
    const { statistic } = this.props;
    const formattedString = formatMeasure(statistic.value, 'SHORT_INT');
    const value = parseFloat(formattedString.slice(0, -1));
    const suffix = formattedString.substr(-1);
    return (
      <div className="sc-stat-card sc-big-spacer-top" ref={node => (this.container = node)}>
        <div className="sc-stat-icon">
          <img alt="" height={28} src={`${getBaseUrl()}/images/sonarcloud/${statistic.icon}.svg`} />
        </div>
        <div className="sc-stat-content">
          {this.state.viewable && (
            <CountUp delay={0} duration={4} end={value} suffix={suffix}>
              {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
                <h5 ref={data.countUpRef}>0</h5>
              )}
            </CountUp>
          )}
          <span>{statistic.text}</span>
        </div>
      </div>
    );
  }
}
