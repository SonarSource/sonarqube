/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {
  ButtonPrimary,
  FlagMessage,
  HelperHintIcon,
  InputSelect,
  LargeCenteredLayout,
  LightLabel,
  Link,
  PageContentFontWrapper,
  PageTitle,
  RadioButton,
  Spinner,
  Title,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { OptionProps, components } from 'react-select';
import A11ySkipTarget from '../../components/a11y/A11ySkipTarget';
import DisableableSelectOption from '../../components/common/DisableableSelectOption';
import HelpTooltip from '../../components/controls/HelpTooltip';
import { LabelValueSelectOption } from '../../components/controls/Select';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { translate } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { getQualityGateUrl } from '../../helpers/urls';
import { QualityGate } from '../../types/types';
import BuiltInQualityGateBadge from '../quality-gates/components/BuiltInQualityGateBadge';
import { USE_SYSTEM_DEFAULT } from './constants';

export interface ProjectQualityGateAppRendererProps {
  allQualityGates?: QualityGate[];
  currentQualityGate?: QualityGate;
  loading: boolean;
  onSelect: (id: string) => void;
  onSubmit: () => void;
  selectedQualityGateName: string;
  submitting: boolean;
}

function hasConditionOnNewCode(qualityGate: QualityGate): boolean {
  return !!qualityGate.conditions?.some((condition) => isDiffMetric(condition.metric));
}

interface QualityGateOption extends LabelValueSelectOption {
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
                  <Link to={getQualityGateUrl(props.data.label)}>
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
  const { allQualityGates, currentQualityGate, loading, selectedQualityGateName, submitting } =
    props;
  const defaultQualityGate = allQualityGates?.find((g) => g.isDefault);

  if (loading) {
    return <Spinner />;
  }

  if (
    allQualityGates === undefined ||
    defaultQualityGate === undefined ||
    currentQualityGate === undefined
  ) {
    return null;
  }

  const usesDefault = selectedQualityGateName === USE_SYSTEM_DEFAULT;
  const needsReanalysis = usesDefault
    ? // currentQualityGate.isDefault is not always up to date. We need to check
      // against defaultQualityGate explicitly.
      defaultQualityGate.name !== currentQualityGate.name
    : selectedQualityGateName !== currentQualityGate.name;

  const selectedQualityGate = allQualityGates.find((qg) => qg.name === selectedQualityGateName);

  const options: QualityGateOption[] = allQualityGates.map((g) => ({
    isDisabled: g.conditions === undefined || g.conditions.length === 0,
    label: g.name,
    value: g.name,
  }));

  return (
    <LargeCenteredLayout id="project-quality-gate">
      <PageContentFontWrapper className="sw-my-8 sw-body-sm">
        <Suggestions suggestions="project_quality_gate" />
        <Helmet defer={false} title={translate('project_quality_gate.page')} />
        <A11ySkipTarget anchor="qg_main" />

        <header className="sw-mb-5 sw-flex sw-items-center">
          <Helmet defer={false} title={translate('project_quality_gate.page')} />
          <Title>{translate('project_quality_gate.page')}</Title>
          <HelpTooltip
            className="sw-ml-2 sw-mb-4"
            overlay={translate('quality_gates.projects.help')}
          >
            <HelperHintIcon />
          </HelpTooltip>
        </header>

        <div className="sw-flex sw-flex-col sw-items-start">
          <div>
            <PageTitle as="h2" text={translate('project_quality_gate.subtitle')} />
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              props.onSubmit();
            }}
            id="project_quality_gate"
          >
            <p className="sw-mb-4">{translate('project_quality_gate.page.description')}</p>

            <div className="sw-mb-4">
              <RadioButton
                className="it__project-quality-default sw-items-start"
                checked={usesDefault}
                disabled={submitting}
                onCheck={() => props.onSelect(USE_SYSTEM_DEFAULT)}
                value={USE_SYSTEM_DEFAULT}
              >
                <div>
                  <div className="sw-ml-1 sw-mb-2">
                    {translate('project_quality_gate.always_use_default')}
                  </div>
                  <div>
                    <LightLabel>
                      {translate('current_noun')}:{defaultQualityGate.name}
                      {defaultQualityGate.isBuiltIn && <BuiltInQualityGateBadge />}
                    </LightLabel>
                  </div>
                </div>
              </RadioButton>
            </div>

            <div className="sw-mb-4">
              <RadioButton
                className="it__project-quality-specific sw-items-start sw-mt-1"
                checked={!usesDefault}
                disabled={submitting}
                onCheck={(value: string) => {
                  if (usesDefault) {
                    props.onSelect(value);
                  }
                }}
                value={!usesDefault ? selectedQualityGateName : currentQualityGate.name}
              >
                <div>
                  <div className="sw-ml-1 sw-mb-2">
                    {translate('project_quality_gate.always_use_specific')}
                  </div>
                </div>
              </RadioButton>
              <div className="sw-ml-6">
                <InputSelect
                  size="large"
                  className="it__project-quality-gate-select"
                  components={{
                    Option: renderQualitygateOption,
                  }}
                  isClearable={usesDefault}
                  isDisabled={submitting || usesDefault}
                  onChange={({ value }: QualityGateOption) => {
                    props.onSelect(value);
                  }}
                  aria-label={translate('project_quality_gate.select_specific_qg')}
                  options={options}
                  value={options.find((o) => o.value === selectedQualityGateName)}
                />
              </div>

              {selectedQualityGate && !hasConditionOnNewCode(selectedQualityGate) && (
                <FlagMessage variant="warning">
                  <FormattedMessage
                    id="project_quality_gate.no_condition_on_new_code"
                    defaultMessage={translate('project_quality_gate.no_condition_on_new_code')}
                    values={{
                      link: (
                        <Link to={getQualityGateUrl(selectedQualityGate.name)}>
                          {translate('project_quality_gate.no_condition.link')}
                        </Link>
                      ),
                    }}
                  />
                </FlagMessage>
              )}
              {needsReanalysis && (
                <FlagMessage className="big-spacer-top abs-width-600" variant="warning">
                  {translate('project_quality_gate.requires_new_analysis')}
                </FlagMessage>
              )}
            </div>

            <div>
              <ButtonPrimary form="project_quality_gate" disabled={submitting} type="submit">
                {translate('save')}
              </ButtonPrimary>
              <Spinner loading={submitting} />
            </div>
          </form>
        </div>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
