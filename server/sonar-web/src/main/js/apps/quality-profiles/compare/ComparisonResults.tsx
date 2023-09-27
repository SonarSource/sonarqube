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
import { ActionCell, ContentCell, Link, Table, TableRowInteractive } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { CompareResponse, Profile } from '../../../api/quality-profiles';
import { getRulesUrl } from '../../../helpers/urls';
import { Dict } from '../../../types/types';
import ComparisonResultActivation from './ComparisonResultActivation';

type Params = Dict<string>;

interface Props extends CompareResponse {
  leftProfile: Profile;
  refresh: () => Promise<void>;
  rightProfile?: Profile;
}

export default function ComparisonResults(props: Readonly<Props>) {
  const { leftProfile, rightProfile, inLeft, left, right, inRight, modified } = props;

  const intl = useIntl();

  const emptyComparison = !inLeft.length && !inRight.length && !modified.length;

  const canActivate = (profile: Profile) =>
    !profile.isBuiltIn && profile.actions && profile.actions.edit;

  const renderRule = React.useCallback((rule: { key: string; name: string }) => {
    return (
      <div>
        <Link className="sw-ml-1" to={getRulesUrl({ rule_key: rule.key, open: rule.key })}>
          {rule.name}
        </Link>
      </div>
    );
  }, []);

  const renderParameters = React.useCallback((params: Params) => {
    if (!params) {
      return null;
    }
    return (
      <ul>
        {Object.keys(params).map((key) => (
          <li className="sw-mt-2 sw-break-all" key={key}>
            <code className="sw-code">
              {key}
              {': '}
              {params[key]}
            </code>
          </li>
        ))}
      </ul>
    );
  }, []);

  const renderLeft = () => {
    if (inLeft.length === 0) {
      return null;
    }

    const renderSecondColumn = rightProfile && canActivate(rightProfile);

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
            {renderSecondColumn && (
              <ContentCell aria-label={intl.formatMessage({ id: 'actions' })}>&nbsp;</ContentCell>
            )}
          </TableRowInteractive>
        }
      >
        {inLeft.map((rule) => (
          <TableRowInteractive key={`left-${rule.key}`}>
            <ContentCell>{renderRule(rule)}</ContentCell>
            {renderSecondColumn && (
              <ContentCell className="sw-px-0">
                <ComparisonResultActivation
                  key={rule.key}
                  onDone={props.refresh}
                  profile={rightProfile}
                  ruleKey={rule.key}
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

    const renderFirstColumn = leftProfile && canActivate(leftProfile);

    return (
      <Table
        columnCount={2}
        columnWidths={['50%', 'auto']}
        noSidePadding
        header={
          <TableRowInteractive>
            {renderFirstColumn && (
              <ContentCell aria-label={intl.formatMessage({ id: 'actions' })}>&nbsp;</ContentCell>
            )}
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
            {renderFirstColumn && (
              <ActionCell className="sw-px-0">
                <ComparisonResultActivation
                  key={rule.key}
                  onDone={props.refresh}
                  profile={leftProfile}
                  ruleKey={rule.key}
                />
              </ActionCell>
            )}
            <ContentCell className="sw-pl-4">{renderRule(rule)}</ContentCell>
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
                {renderRule(rule)}
                {renderParameters(rule.left.params)}
              </div>
            </ContentCell>
            <ContentCell className="sw-pl-4">
              <div>
                {renderRule(rule)}
                {renderParameters(rule.right.params)}
              </div>
            </ContentCell>
          </TableRowInteractive>
        ))}
      </Table>
    );
  };

  return (
    <div className="sw-mt-4">
      {emptyComparison ? (
        intl.formatMessage({ id: 'quality_profile.empty_comparison' })
      ) : (
        <>
          {renderLeft()}
          {renderRight()}
          {renderModified()}
        </>
      )}
    </div>
  );
}
