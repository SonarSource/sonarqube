/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * LGPL v3
 */

import * as React from 'react';
import { ToggleButton } from '~design-system';
import { translate } from '../../helpers/l10n';
import { CvssBreakdown as CvssBreakdownType, CvssMetrics } from '../../types/types';

interface Props {
  cvssBreakdown: CvssBreakdownType;
}

type CvssTabType = 'base' | 'temporal' | 'environmental';

interface State {
  selectedTab: CvssTabType;
}

export default class CvssBreakdown extends React.PureComponent<Props, State> {
  state: State = {
    selectedTab: 'base',
  };

  handleTabChange = (value: CvssTabType) => {
    this.setState({ selectedTab: value });
  };

  /* =========================
     SCORE SUMMARY (INLINE)
     ========================= */
  renderScoreSummary = () => {
    const { scores } = this.props.cvssBreakdown;
    return (
      <div className="sw-flex sw-gap-6 sw-mb-6">
        {scores.overall !== undefined && (
          <span className="sw-text-sm">
            <span className="sw-text-muted">CVSS Score:</span>{' '}
            <span style={{ color: 'rgb(93, 108, 208)' }}className="sw-font-semibold">{scores.overall.toFixed(1)}</span>
          </span>
        )}

        {scores.base !== undefined && (
          <span className="sw-text-sm">
            <span className="sw-text-muted">Base Score:</span>{' '}
            <span style={{ color: 'rgb(93, 108, 208)' }}className="sw-font-semibold">{scores.base.toFixed(1)}</span>
          </span>
        )}

        {scores.temporal !== undefined && (
          <span className="sw-text-sm">
            <span className="sw-text-muted">Temporal Score:</span>{' '}
            <span style={{ color: 'rgb(93, 108, 208)' }}className="sw-font-semibold">{scores.temporal.toFixed(1)}</span>
          </span>
        )}

        {scores.environmental !== undefined && (
          <span className="sw-text-sm">
            <span className="sw-text-muted">Environmental Score:</span>{' '}
            <span style={{ color: 'rgb(93, 108, 208)' }}  className="sw-font-semibold">
              {scores.environmental.toFixed(1)}
            </span>
          </span>
        )}
      </div>
    );
  };

  /* =========================
     METRICS (LIST STYLE)
     ========================= */
renderMetricsTable = (metrics?: CvssMetrics) => {
  if (!metrics?.metrics?.length) {
    return null;
  }

return (
  <div className="sw-mt-3 sw-max-w-[1100px]">
    {/* Header */}
    <div
      className="
        sw-grid
        sw-grid-cols-[260px_140px_auto]
        sw-gap-4
        sw-py-2
        sw-text-sm
        sw-font-semibold
      "
    >
      <div>Metric Name</div>
      <div>Value</div>
      <div>Justification</div>
    </div>

    <hr className="sw-border-0 sw-border-b sw-border-default sw-mb-2" />

    {/* Rows */}
    {metrics.metrics.map((metric, index) => (
      <React.Fragment key={index}>
        <div
          className="
            sw-grid
            sw-grid-cols-[260px_140px_auto]
            sw-gap-4
            sw-py-3
            sw-text-sm
          "
        >
          <div style={{ fontWeight: 'bold', color: 'rgb(93, 108, 208)' }}>
            {metric.name}
          </div>

          <div>{metric.value}</div>

          <div
            style={{ fontWeight: 'bold', color: 'rgb(93, 108, 208)' }}
            className="sw-break-words sw-whitespace-pre-line sw-text-justify"
          >
            {metric.justification || '-'}
          </div>
        </div>

        {/* Full-width row separator */}
        <hr className="sw-border-0 sw-border-b sw-border-default" />
      </React.Fragment>
    ))}
  </div>
);
};


  render() {
    const { cvssBreakdown } = this.props;
    const { selectedTab } = this.state;

    const tabs = [
      { value: 'base' as CvssTabType, label: translate('coding_rules.cvss_breakdown.tab.base') },
      { value: 'temporal' as CvssTabType, label: translate('coding_rules.cvss_breakdown.tab.temporal') },
      { value: 'environmental' as CvssTabType, label: translate('coding_rules.cvss_breakdown.tab.environmental') },
    ].filter(tab => cvssBreakdown[tab.value]);

    const selectedMetrics =
      selectedTab === 'base'
        ? cvssBreakdown.base
        : selectedTab === 'temporal'
        ? cvssBreakdown.temporal
        : cvssBreakdown.environmental;

    return (
      <div className="sw-px-1 sw-py-6">
        {this.renderScoreSummary()}
        {tabs.length > 1 && (
          <div className="sw-mb-6">
            <ToggleButton
              role="tablist"
              value={selectedTab}
              options={tabs}
              onChange={this.handleTabChange}
            />
          </div>
        )}

        {this.renderMetricsTable(selectedMetrics)}
      </div>
    );
  }
}
