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
import { ActionCell, ContentCell, Link, Table, TableRowInteractive } from 'design-system';
import { isEqual } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { CompareResponse, Profile, RuleCompare } from '../../../api/quality-profiles';
import IssueSeverityIcon from '../../../components/icon-mappers/IssueSeverityIcon';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SoftwareImpactPillList from '../../../components/shared/SoftwareImpactPillList';
import { getRulesUrl } from '../../../helpers/urls';
import { IssueSeverity } from '../../../types/issues';
import { Dict } from '../../../types/types';
import ComparisonResultActivation from './ComparisonResultActivation';
import ComparisonResultDeactivation from './ComparisonResultDeactivation';
import ComparisonResultsSummary from './ComparisonResultsSummary';

type Params = Dict<string>;

interface Props extends CompareResponse {
  canDeactivateInheritedRules: boolean;
  leftProfile: Profile;
  refresh: () => Promise<void>;
  rightProfile?: Profile;
}

export default function ComparisonResults(props: Readonly<Props>) {
  const {
    leftProfile,
    rightProfile,
    inLeft,
    left,
    right,
    inRight,
    modified,
    canDeactivateInheritedRules,
  } = props;

  const intl = useIntl();

  const emptyComparison = !inLeft.length && !inRight.length && !modified.length;

  const canEdit = (profile: Profile) => !profile.isBuiltIn && profile.actions?.edit;

  const renderLeft = () => {
    if (inLeft.length === 0) {
      return null;
    }

    const canRenderSecondColumn = leftProfile && canEdit(leftProfile);
    return (
      <Table
        columnCount={2}
        columnWidths={['50%', 'auto']}
        noSidePadding
        header={
          <TableRowInteractive>
            <ContentCell>
              {intl.formatMessage(
                {
                  id: 'quality_profiles.x_rules_only_in',
                },
                { count: inLeft.length, profile: left.name },
              )}
            </ContentCell>
            {canRenderSecondColumn && (
              <ContentCell aria-label={intl.formatMessage({ id: 'actions' })}>&nbsp;</ContentCell>
            )}
          </TableRowInteractive>
        }
      >
        {inLeft.map((rule) => (
          <TableRowInteractive key={`left-${rule.key}`}>
            <ContentCell>
              <RuleCell rule={rule} />
            </ContentCell>
            {canRenderSecondColumn && (
              <ContentCell className="sw-px-0">
                <ComparisonResultDeactivation
                  key={rule.key}
                  onDone={props.refresh}
                  profile={leftProfile}
                  ruleKey={rule.key}
                  canDeactivateInheritedRules={canDeactivateInheritedRules}
                />
              </ContentCell>
            )}
          </TableRowInteractive>
        ))}
      </Table>
    );
  };

  const renderRight = () => {
    if (inRight.length === 0) {
      return null;
    }

    const renderFirstColumn = leftProfile && canEdit(leftProfile);

    return (
      <Table
        columnCount={2}
        columnWidths={['50%', 'auto']}
        noSidePadding
        header={
          <TableRowInteractive>
            <ContentCell aria-label={intl.formatMessage({ id: 'actions' })}>&nbsp;</ContentCell>
            <ContentCell className="sw-pl-4">
              {intl.formatMessage(
                {
                  id: 'quality_profiles.x_rules_only_in',
                },
                { count: inRight.length, profile: right.name },
              )}
            </ContentCell>
          </TableRowInteractive>
        }
      >
        {inRight.map((rule) => (
          <TableRowInteractive key={`right-${rule.key}`}>
            <ActionCell className="sw-px-0">
              {renderFirstColumn && (
                <ComparisonResultActivation
                  key={rule.key}
                  onDone={props.refresh}
                  profile={leftProfile}
                  ruleKey={rule.key}
                />
              )}
            </ActionCell>
            <ContentCell className="sw-pl-4">
              <RuleCell rule={rule} />
            </ContentCell>
          </TableRowInteractive>
        ))}
      </Table>
    );
  };

  const renderModified = () => {
    if (modified.length === 0) {
      return null;
    }

    return (
      <Table
        columnCount={2}
        columnWidths={['50%', 'auto']}
        noSidePadding
        header={
          <TableRowInteractive>
            <ContentCell>{left.name}</ContentCell>
            <ContentCell className="sw-pl-4">{right.name}</ContentCell>
          </TableRowInteractive>
        }
        caption={
          <>
            {intl.formatMessage(
              { id: 'quality_profiles.x_rules_have_different_configuration' },
              { count: modified.length },
            )}
          </>
        }
      >
        {modified.map((rule) => (
          <TableRowInteractive key={`modified-${rule.key}`}>
            <ContentCell>
              <div>
                <RuleCell rule={rule} severity={rule.left.severity} />
                <Parameters params={rule.left.params} />
              </div>
            </ContentCell>
            <ContentCell className="sw-pl-4">
              <div>
                <RuleCell rule={rule} severity={rule.right.severity} />
                <Parameters params={rule.right.params} />
              </div>
            </ContentCell>
          </TableRowInteractive>
        ))}
      </Table>
    );
  };

  return (
    <div className="sw-mt-8">
      {emptyComparison ? (
        intl.formatMessage({ id: 'quality_profile.empty_comparison' })
      ) : (
        <>
          <ComparisonResultsSummary
            profileName={leftProfile.name}
            comparedProfileName={rightProfile?.name}
            additionalCount={inLeft.length}
            fewerCount={inRight.length}
          />
          {renderLeft()}
          {renderRight()}
          {renderModified()}
        </>
      )}
    </div>
  );
}

function RuleCell({ rule, severity }: Readonly<{ rule: RuleCompare; severity?: string }>) {
  const shouldRenderSeverity =
    Boolean(severity) && rule.left && rule.right && isEqual(rule.left.params, rule.right.params);

  return (
    <div>
      {shouldRenderSeverity && <IssueSeverityIcon severity={severity as IssueSeverity} />}
      <Link className="sw-ml-1" to={getRulesUrl({ rule_key: rule.key, open: rule.key })}>
        {rule.name}
      </Link>
      {(rule.cleanCodeAttributeCategory || rule.impacts.length > 0) && (
        <ul className="sw-mt-3 sw-flex sw-items-center">
          {rule.cleanCodeAttributeCategory && (
            <li>
              <CleanCodeAttributePill
                cleanCodeAttributeCategory={rule.cleanCodeAttributeCategory}
              />
            </li>
          )}
          {rule.impacts.length > 0 && (
            <li>
              <SoftwareImpactPillList className="sw-ml-2" softwareImpacts={rule.impacts} />
            </li>
          )}
        </ul>
      )}
    </div>
  );
}

function Parameters({ params }: Readonly<{ params?: Params }>) {
  if (!params) {
    return null;
  }

  return (
    <ul>
      {Object.keys(params).map((key) => (
        <li className="sw-mt-2 sw-break-all" key={key}>
          <code className="sw-typo-default">
            {key}
            {': '}
            {params[key]}
          </code>
        </li>
      ))}
    </ul>
  );
}
