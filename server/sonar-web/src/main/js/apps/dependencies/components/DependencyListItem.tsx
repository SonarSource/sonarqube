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

import { IconArrowRight, LinkStandalone, Text } from '@sonarsource/echoes-react';
import { Card, Pill, PillHighlight, PillVariant } from 'design-system';
import React from 'react';

import { FormattedMessage } from 'react-intl';
import SoftwareImpactSeverityIcon from '../../../components/icon-mappers/SoftwareImpactSeverityIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { Dependency } from '../../../types/dependencies';

function DependencyListItem({ dependency }: Readonly<{ dependency: Dependency }>) {
  //TODO: Clean up once the response returns these values always
  const findingsExist = isDefined(dependency.findingsCount);
  const findingsCount = isDefined(dependency.findingsCount) ? dependency.findingsCount : 0;
  const findingsExploitableCount = dependency.findingsExploitableCount ?? 0;

  const hasFindings = findingsCount > 0;

  return (
    <Card className="sw-p-3 sw-mb-4">
      <div className="sw-flex sw-justify-between sw-items-center">
        <div className="sw-flex sw-items-center">
          <span className="sw-w-[305px] sw-inline-flex sw-overflow-hidden">
            <span className="sw-flex-shrink sw-overflow-hidden sw-text-ellipsis sw-whitespace-nowrap">
              {hasFindings ? (
                <LinkStandalone
                  to={`/dependencies/${dependency.key}`}
                  className="sw-mr-2 sw-text-sm"
                >
                  {dependency.name}
                </LinkStandalone>
              ) : (
                <Text isHighlighted isSubdued className="sw-mr-2">
                  {dependency.name}
                </Text>
              )}
            </span>
            <Pill
              variant={PillVariant.Accent}
              highlight={PillHighlight.Medium}
              className="sw-flex-shrink-0 sw-mr-2"
            >
              {dependency.transitive
                ? translate('dependencies.direct.label')
                : translate('dependencies.transitive.label')}
            </Pill>
          </span>
          {findingsExist && (
            <span className="sw-flex">
              {hasFindings ? (
                <>
                  <Text isHighlighted className="sw-mr-2">
                    {translateWithParameters(
                      'dependencies.dependency.findings.label',
                      findingsCount,
                    )}
                  </Text>
                  {Object.entries(dependency.findingsSeverities || {}).map(([severity, count]) => (
                    <span key={severity} className="sw-flex sw-items-center sw-mr-1">
                      <SoftwareImpactSeverityIcon
                        severity={severity}
                        className="sw-mr-1"
                        width={16}
                        height={16}
                      />
                      <Text>{count}</Text>
                    </span>
                  ))}
                  {findingsExploitableCount > 0 && (
                    <Pill
                      variant={PillVariant.Danger}
                      highlight={PillHighlight.Medium}
                      className="sw-ml-2"
                    >
                      <FormattedMessage
                        id="dependencies.dependency.exploitable_findings.label"
                        defaultMessage={translate(
                          'dependencies.dependency.exploitable_findings.label',
                        )}
                        values={{
                          count: findingsExploitableCount,
                        }}
                      />
                    </Pill>
                  )}
                </>
              ) : (
                <Text isSubdued>{translate('dependencies.dependency.no_findings.label')}</Text>
              )}
            </span>
          )}
        </div>
        <div className="sw-flex sw-items-center">
          {isDefined(dependency.fixVersion) ? (
            <>
              <Text className="sw-mr-1">{translate('dependencies.dependency.version.label')}</Text>
              <Pill variant={PillVariant.Caution} highlight={PillHighlight.Medium}>
                {dependency.version}
              </Pill>
            </>
          ) : (
            <Pill variant={PillVariant.Neutral} highlight={PillHighlight.Medium}>
              {dependency.version}
            </Pill>
          )}

          {isDefined(dependency.fixVersion) && (
            <>
              <IconArrowRight />
              <Text className="sw-mr-1">
                {translate('dependencies.dependency.fix_version.label')}
              </Text>
              <Pill variant={PillVariant.Success} highlight={PillHighlight.Medium}>
                {dependency.fixVersion}
              </Pill>
            </>
          )}
        </div>
      </div>
    </Card>
  );
}

export default DependencyListItem;
