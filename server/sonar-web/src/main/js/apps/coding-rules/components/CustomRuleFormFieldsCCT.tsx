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

import { Checkbox, Select, Text } from '@sonarsource/echoes-react';
import { useEffect, useMemo, useRef } from 'react';
import { useIntl } from 'react-intl';
import { FormField, RequiredIcon } from '~design-system';
import SoftwareImpactSeverityIcon from '../../../components/icon-mappers/SoftwareImpactSeverityIcon';
import {
  CLEAN_CODE_ATTRIBUTES_BY_CATEGORY,
  CLEAN_CODE_CATEGORIES,
  IMPACT_SEVERITIES,
  SOFTWARE_QUALITIES,
} from '../../../helpers/constants';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpact,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';

interface Props<T> {
  disabled?: boolean;
  onChange: (value: T) => void;
  value: T;
}

export function CleanCodeCategoryField(props: Readonly<Props<CleanCodeAttributeCategory>>) {
  const { value, disabled } = props;
  const intl = useIntl();

  const categories = CLEAN_CODE_CATEGORIES.map((category) => ({
    value: category,
    label: intl.formatMessage({ id: `rule.clean_code_attribute_category.${category}` }),
  }));

  return (
    <FormField
      ariaLabel={intl.formatMessage({ id: 'category' })}
      label={intl.formatMessage({ id: 'category' })}
      htmlFor="coding-rules-custom-clean-code-category"
    >
      <Select
        data={categories}
        id="coding-rules-custom-clean-code-category"
        onChange={(option) =>
          option ? props.onChange(option as CleanCodeAttributeCategory) : undefined
        }
        isDisabled={disabled}
        isSearchable={false}
        isNotClearable
        value={categories.find((category) => category.value === value)?.value}
      />
    </FormField>
  );
}

export function CleanCodeAttributeField(
  props: Readonly<Props<CleanCodeAttribute> & { category: CleanCodeAttributeCategory }>,
) {
  const { value, disabled, category, onChange } = props;
  const initialAttribute = useRef(value);
  const intl = useIntl();

  const attributes = CLEAN_CODE_ATTRIBUTES_BY_CATEGORY[category].map((attribute) => ({
    value: attribute,
    label: intl.formatMessage({ id: `rule.clean_code_attribute.${attribute}` }),
  }));

  // Set default CC attribute when category changes
  useEffect(() => {
    if (CLEAN_CODE_ATTRIBUTES_BY_CATEGORY[category].includes(value)) {
      return;
    }
    const initialAttributeIndex = CLEAN_CODE_ATTRIBUTES_BY_CATEGORY[category].findIndex(
      (attribute) => attribute === initialAttribute.current,
    );
    onChange(
      CLEAN_CODE_ATTRIBUTES_BY_CATEGORY[category][
        initialAttributeIndex === -1 ? 0 : initialAttributeIndex
      ],
    );
  }, [onChange, category, value]);

  return (
    <FormField
      ariaLabel={intl.formatMessage({ id: 'attribute' })}
      label={intl.formatMessage({ id: 'attribute' })}
      htmlFor="coding-rules-custom-clean-code-attribute"
    >
      <Select
        data={attributes}
        id="coding-rules-custom-clean-code-attribute"
        onChange={(option) => props.onChange(option as CleanCodeAttribute)}
        isDisabled={disabled}
        isSearchable={false}
        isNotClearable
        value={attributes.find((attribute) => attribute.value === value)?.value}
      />
    </FormField>
  );
}

export function SoftwareQualitiesFields(
  props: Readonly<Props<SoftwareImpact[]> & { error: boolean; qualityUpdateDisabled: boolean }>,
) {
  const { value, disabled, error, qualityUpdateDisabled } = props;
  const intl = useIntl();

  const severities = useMemo(
    () =>
      IMPACT_SEVERITIES.map((severity) => ({
        value: severity,
        label: intl.formatMessage({ id: `severity_impact.${severity}` }),
        prefix: <SoftwareImpactSeverityIcon severity={severity} />,
      })),
    [intl],
  );

  const handleSoftwareQualityChange = (quality: SoftwareQuality, checked: boolean | string) => {
    if (checked === true) {
      props.onChange([
        ...value,
        { softwareQuality: quality, severity: SoftwareImpactSeverity.Low },
      ]);
    } else {
      props.onChange(value.filter((impact) => impact.softwareQuality !== quality));
    }
  };

  const handleSeverityChange = (quality: SoftwareQuality, severity: SoftwareImpactSeverity) => {
    props.onChange(
      value.map((impact) =>
        impact.softwareQuality === quality ? { ...impact, severity } : impact,
      ),
    );
  };

  return (
    <fieldset className="sw-mt-2 sw-mb-4 sw-relative">
      <legend className="sw-w-full sw-flex sw-justify-between sw-gap-6 sw-mb-4">
        <Text isHighlighted className="sw-w-full">
          {intl.formatMessage({ id: 'software_quality' })}
          <RequiredIcon aria-label={intl.formatMessage({ id: 'required' })} className="sw-ml-1" />
        </Text>
        <Text isHighlighted className="sw-w-full">
          {intl.formatMessage({ id: 'severity' })}
          <RequiredIcon aria-label={intl.formatMessage({ id: 'required' })} className="sw-ml-1" />
        </Text>
      </legend>
      {SOFTWARE_QUALITIES.map((quality) => {
        const selectedQuality = value.find((impact) => impact.softwareQuality === quality);
        const selectedSeverity = selectedQuality
          ? severities.find((severity) => severity.value === selectedQuality.severity)?.value
          : null;

        return (
          <fieldset key={quality} className="sw-flex sw-justify-between sw-gap-6 sw-mb-4">
            <legend className="sw-sr-only">
              {intl.formatMessage(
                { id: 'coding_rules.custom_rule.software_quality_x' },
                { quality },
              )}
            </legend>
            <Checkbox
              className="sw-w-full sw-items-center"
              isDisabled={qualityUpdateDisabled}
              checked={Boolean(selectedQuality)}
              onCheck={(checked) => {
                handleSoftwareQualityChange(quality, checked);
              }}
              label={
                <Text className="sw-ml-3">
                  {intl.formatMessage({ id: `software_quality.${quality}` })}
                </Text>
              }
            />

            <Select
              id={`coding-rules-custom-software-impact-severity-${quality}`}
              aria-label={intl.formatMessage({ id: 'severity' })}
              className="sw-w-full"
              data={severities}
              placeholder={intl.formatMessage({ id: 'none' })}
              onChange={(option) => handleSeverityChange(quality, option as SoftwareImpactSeverity)}
              isDisabled={disabled || !selectedQuality}
              isSearchable={false}
              isNotClearable
              value={selectedSeverity}
              valueIcon={<SoftwareImpactSeverityIcon severity={selectedSeverity} />}
            />
          </fieldset>
        );
      })}
      {error && (
        <Text
          colorOverride="echoes-color-text-danger"
          className="sw-font-regular sw-absolute sw--bottom-3"
        >
          {intl.formatMessage({ id: 'coding_rules.custom_rule.select_software_quality' })}
        </Text>
      )}
    </fieldset>
  );
}
