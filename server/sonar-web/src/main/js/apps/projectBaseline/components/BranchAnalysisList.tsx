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
import { subDays } from 'date-fns';
import { throttle } from 'lodash';
import * as React from 'react';
import { getProjectActivity } from '../../../api/projectActivity';
import { parseDate, toShortNotSoISOString } from '../../../helpers/dates';
import { scrollToElement } from '../../../helpers/scrolling';
import { Analysis, ParsedAnalysis } from '../../../types/project-activity';
import { Dict } from '../../../types/types';
import BranchAnalysisListRenderer from './BranchAnalysisListRenderer';

interface Props {
  analysis: string;
  branch: string;
  component: string;
  onSelectAnalysis: (analysis: ParsedAnalysis) => void;
}

interface State {
  analyses: ParsedAnalysis[];
  loading: boolean;
  range: number;
  scroll: number;
}

const STICKY_BADGE_SCROLL_OFFSET = 10;

export default class BranchAnalysisList extends React.PureComponent<Props, State> {
  mounted = false;
  badges: Dict<HTMLDivElement> = {};
  scrollableNode?: HTMLDivElement;
  state: State = {
    analyses: [],
    loading: true,
    range: 30,
    scroll: 0,
  };

  constructor(props: Props) {
    super(props);
    this.updateScroll = throttle(this.updateScroll, 20);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchAnalyses(true);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  scrollToSelected() {
    const selectedNode = document.querySelector('.branch-analysis.selected');
    if (this.scrollableNode && selectedNode) {
      scrollToElement(selectedNode, { parent: this.scrollableNode, bottomOffset: 40 });
    }
  }

  fetchAnalyses(initial = false) {
    const { analysis, branch, component } = this.props;
    const { range } = this.state;
    this.setState({ loading: true });

    return getProjectActivity({
      branch,
      project: component,
      from: range ? toShortNotSoISOString(subDays(new Date(), range)) : undefined,
    }).then((result: { analyses: Analysis[] }) => {
      // If the selected analysis wasn't found in the default 30 days range, redo the search
      if (initial && analysis && !result.analyses.find((a) => a.key === analysis)) {
        this.handleRangeChange({ value: 0 });
        return;
      }

      this.setState(
        {
          analyses: result.analyses.map((analysis) => ({
            ...analysis,
            date: parseDate(analysis.date),
          })) as ParsedAnalysis[],
          loading: false,
        },
        () => {
          this.scrollToSelected();
        }
      );
    });
  }

  handleScroll = (e: React.SyntheticEvent<HTMLDivElement>) => {
    if (e.currentTarget) {
      this.updateScroll(e.currentTarget.scrollTop);
    }
  };

  updateScroll = (scroll: number) => {
    this.setState({ scroll });
  };

  registerBadgeNode = (version: string) => (el: HTMLDivElement) => {
    if (el) {
      if (!el.getAttribute('originOffsetTop')) {
        el.setAttribute('originOffsetTop', String(el.offsetTop));
      }
      this.badges[version] = el;
    }
  };

  shouldStick = (version: string) => {
    const badge = this.badges[version];
    return (
      !!badge &&
      Number(badge.getAttribute('originOffsetTop')) < this.state.scroll + STICKY_BADGE_SCROLL_OFFSET
    );
  };

  handleRangeChange = ({ value }: { value: number }) => {
    this.setState({ range: value }, () => this.fetchAnalyses());
  };

  render() {
    const { analysis, onSelectAnalysis } = this.props;
    const { analyses, loading, range } = this.state;

    return (
      <BranchAnalysisListRenderer
        analyses={analyses}
        handleRangeChange={this.handleRangeChange}
        handleScroll={this.handleScroll}
        loading={loading}
        onSelectAnalysis={onSelectAnalysis}
        range={range}
        registerBadgeNode={this.registerBadgeNode}
        registerScrollableNode={(el) => {
          this.scrollableNode = el;
        }}
        selectedAnalysisKey={analysis}
        shouldStick={this.shouldStick}
      />
    );
  }
}
