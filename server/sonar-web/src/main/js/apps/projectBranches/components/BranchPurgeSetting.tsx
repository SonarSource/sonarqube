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
import { HelperHintIcon, Spinner, Switch } from 'design-system';
import * as React from 'react';
import { useEffect } from 'react';
import { isMainBranch } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { useExcludeFromPurgeMutation } from '../../../queries/branch';
import HelpTooltip from '../../../sonar-aligned/components/controls/HelpTooltip';
import { Branch } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branch: Branch;
  component: Component;
}

export default function BranchPurgeSetting(props: Props) {
  const { branch, component } = props;
  const { mutate: excludeFromPurge, isPending } = useExcludeFromPurgeMutation();

  useEffect(() => {
    excludeFromPurge({ component, key: branch.name, exclude: branch.excludedFromPurge });
  }, [branch.excludedFromPurge]);

  const handleOnChange = (exclude: boolean) => {
    excludeFromPurge({ component, key: branch.name, exclude });
  };

  const isTheMainBranch = isMainBranch(branch);
  const disabled = isTheMainBranch || isPending;

  return (
    <>
      <Switch
        disabled={disabled}
        onChange={handleOnChange}
        value={branch.excludedFromPurge}
        labels={{
          on: translate('on'),
          off: translate('off'),
        }}
      />
      <Spinner loading={isPending} className="sw-ml-1" />
      {isTheMainBranch && (
        <HelpTooltip
          className="sw-ml-1"
          overlay={translate(
            'project_branch_pull_request.branch.auto_deletion.main_branch_tooltip',
          )}
        >
          <HelperHintIcon aria-label={translate('help')} />
        </HelpTooltip>
      )}
    </>
  );
}
