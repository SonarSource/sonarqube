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
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { components, OptionProps } from 'react-select';
import A11ySkipTarget from '../../components/a11y/A11ySkipTarget';
import DisableableSelectOption from '../../components/common/DisableableSelectOption';
import Link from '../../components/common/Link';
import { SubmitButton } from '../../components/controls/buttons';
import HelpTooltip from '../../components/controls/HelpTooltip';
import Radio from '../../components/controls/Radio';
import Select, { BasicSelectOption } from '../../components/controls/Select';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { Alert } from '../../components/ui/Alert';
import { translate } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { QualityGate } from '../../types/types';
import BuiltInQualityGateBadge from '../quality-gates/components/BuiltInQualityGateBadge';
import { USE_SYSTEM_DEFAULT } from './constants';

export interface ProjectQualityGateAppRendererProps {
  allQualityGates?: QualityGate[];
  currentQualityGate?: QualityGate;
  loading: boolean;
  onSelect: (id: string) => void;
  onSubmit: () => void;
  selectedQualityGateId: string;
  submitting: boolean;
}

function hasConditionOnNewCode(qualityGate: QualityGate): boolean {
  return !!qualityGate.conditions?.some((condition) => isDiffMetric(condition.metric));
}

interface QualityGateOption extends BasicSelectOption {
  isDisabled: boolean;
}

function renderQualitygateOption(props: OptionProps<QualityGateOption, false>) {
  return (
    <components.Option {...props}>
      <div>
        <DisableableSelectOption
          className="abs-width-100"
          option={props.data}
          disabledReason={translate('project_quality_gate.no_condition.reason')}
          disableTooltipOverlay={() => (
            <FormattedMessage
              id="project_quality_gate.no_condition"
              defaultMessage={translate('project_quality_gate.no_condition')}
              values={{
                link: (
                  <Link to={{ pathname: `/quality_gates/show/${props.data.value}` }}>
                    {translate('project_quality_gate.no_condition.link')}
                  </Link>
                ),
              }}
            />
          )}
        />
      </div>
    </components.Option>
  );
}

export default function ProjectQualityGateAppRenderer(props: ProjectQualityGateAppRendererProps) {
  const { allQualityGates, currentQualityGate, loading, selectedQualityGateId, submitting } = props;
  const defaultQualityGate = allQualityGates?.find((g) => g.isDefault);

  if (loading) {
    return <i className="spinner" />;
  }

  if (
    allQualityGates === undefined ||
    defaultQualityGate === undefined ||
    currentQualityGate === undefined
  ) {
    return null;
  }

  const usesDefault = selectedQualityGateId === USE_SYSTEM_DEFAULT;
  const needsReanalysis = usesDefault
    ? // currentQualityGate.isDefault is not always up to date. We need to check
      // against defaultQualityGate explicitly.
      defaultQualityGate.id !== currentQualityGate.id
    : selectedQualityGateId !== currentQualityGate.id;

  const selectedQualityGate = allQualityGates.find((qg) => qg.id === selectedQualityGateId);

  const options: QualityGateOption[] = allQualityGates.map((g) => ({
    isDisabled: g.conditions === undefined || g.conditions.length === 0,
    label: g.name,
    value: g.id,
  }));

  return (
    <div className="page page-limited" id="project-quality-gate">
      <Suggestions suggestions="project_quality_gate" />
      <Helmet defer={false} title={translate('project_quality_gate.page')} />
      <A11ySkipTarget anchor="qg_main" />

      <header className="page-header">
        <div className="page-title display-flex-center">
          <h1>{translate('project_quality_gate.page')}</h1>
          <HelpTooltip
            className="spacer-left"
            overlay={
              <div className="big-padded-top big-padded-bottom">
                {translate('quality_gates.projects.help')}
              </div>
            }
          />
        </div>
      </header>

      <div className="boxed-group">
        <h2 className="boxed-group-header">{translate('project_quality_gate.subtitle')}</h2>

        <form
          className="boxed-group-inner"
          onSubmit={(e) => {
            e.preventDefault();
            props.onSubmit();
          }}
        >
          <p className="big-spacer-bottom">{translate('project_quality_gate.page.description')}</p>

          <div className="big-spacer-bottom">
            <Radio
              className="display-flex-start"
              checked={usesDefault}
              disabled={submitting}
              onCheck={() => props.onSelect(USE_SYSTEM_DEFAULT)}
              value={USE_SYSTEM_DEFAULT}
            >
              <div className="spacer-left">
                <div className="little-spacer-bottom">
                  {translate('project_quality_gate.always_use_default')}
                </div>
                <div className="display-flex-center">
                  <span className="text-muted little-spacer-right">
                    {translate('current_noun')}:
                  </span>
                  {defaultQualityGate.name}
                  {defaultQualityGate.isBuiltIn && (
                    <BuiltInQualityGateBadge className="spacer-left" />
                  )}
                </div>
              </div>
            </Radio>
          </div>

          <div className="big-spacer-bottom">
            <Radio
              className="display-flex-start"
              checked={!usesDefault}
              disabled={submitting}
              onCheck={(value) => {
                if (usesDefault) {
                  props.onSelect(value);
                }
              }}
              value={!usesDefault ? selectedQualityGateId : currentQualityGate.id}
            >
              <div className="spacer-left">
                <div className="little-spacer-bottom">
                  {translate('project_quality_gate.always_use_specific')}
                </div>
                <div className="display-flex-center">
                  <Select
                    className="abs-width-300 it__project-quality-gate-select"
                    components={{
                      Option: renderQualitygateOption,
                    }}
                    isClearable={usesDefault}
                    isDisabled={submitting || usesDefault}
                    onChange={({ value }: QualityGateOption) => {
                      props.onSelect(value);
                    }}
                    options={options}
                    value={options.find((o) => o.value === selectedQualityGateId)}
                  />
                </div>
              </div>
            </Radio>

            {selectedQualityGate && !hasConditionOnNewCode(selectedQualityGate) && (
              <Alert className="abs-width-600 spacer-top" variant="warning">
                <FormattedMessage
                  id="project_quality_gate.no_condition_on_new_code"
                  defaultMessage={translate('project_quality_gate.no_condition_on_new_code')}
                  values={{
                    link: (
                      <Link to={{ pathname: `/quality_gates/show/${selectedQualityGate.id}` }}>
                        {translate('project_quality_gate.no_condition.link')}
                      </Link>
                    ),
                  }}
                />
              </Alert>
            )}
            {needsReanalysis && (
              <Alert className="big-spacer-top abs-width-600" variant="warning">
                {translate('project_quality_gate.requires_new_analysis')}
              </Alert>
            )}
          </div>

          <div>
            <SubmitButton disabled={submitting}>{translate('save')}</SubmitButton>
            {submitting && <i className="spinner spacer-left" />}
          </div>
        </form>
      </div>
    </div>
  );
}
