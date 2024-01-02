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
import classNames from 'classnames';
import * as React from 'react';
import { BranchLike } from '../../../../../../types/branch-like';
import { getBranches } from '../../../../../../api/branches';
import { getRegulatoryReportUrl } from '../../../../../../api/regulatory-report';
import { ButtonLink } from '../../../../../../components/controls/buttons';
import Select, { BasicSelectOption } from '../../../../../../components/controls/Select';
import {
  getBranchLikeDisplayName,
  isBranch,
  isMainBranch,
} from '../../../../../../helpers/branch-like';
import { translate } from '../../../../../../helpers/l10n';
import { Component } from '../../../../../../types/types';
import { orderBy } from 'lodash';

interface Props {
  component: Pick<Component, 'key' | 'name'>;
  branchLike?: BranchLike;
  onClose: () => void;
}

interface State {
  downloadStarted: boolean;
  selectedBranch: string;
  branchLikesOptions: BasicSelectOption[];
}

export default class RegulatoryReport extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      downloadStarted: false,
      selectedBranch: '',
      branchLikesOptions: [],
    };
  }

  componentDidMount() {
    const { component, branchLike } = this.props;
    getBranches(component.key)
      .then((data) => {
        const mainBranch = data.find(isMainBranch);
        const otherBranchSorted = orderBy(
          data.filter(isBranch).filter((b) => !isMainBranch(b)),
          (b) => b.name
        );
        const sortedBranch = mainBranch ? [mainBranch, ...otherBranchSorted] : otherBranchSorted;
        const options = sortedBranch
          .filter((br) => br.excludedFromPurge)
          .map((br) => {
            return {
              value: getBranchLikeDisplayName(br),
              label: getBranchLikeDisplayName(br),
            };
          });

        let selectedBranch = '';
        if (branchLike && isBranch(branchLike) && branchLike.excludedFromPurge) {
          selectedBranch = getBranchLikeDisplayName(branchLike);
        } else if (mainBranch) {
          selectedBranch = getBranchLikeDisplayName(mainBranch);
        }
        this.setState({ selectedBranch, branchLikesOptions: options });
      })
      .catch(() => {
        this.setState({ branchLikesOptions: [] });
      });
  }

  onBranchSelect = (newOption: BasicSelectOption) => {
    this.setState({ selectedBranch: newOption.value, downloadStarted: false });
  };

  render() {
    const { component, onClose } = this.props;
    const { downloadStarted, selectedBranch, branchLikesOptions } = this.state;

    return (
      <>
        <div className="modal-head">
          <h2>{translate('regulatory_report.page')}</h2>
        </div>
        <div className="modal-body">
          <p>{translate('regulatory_report.description1')}</p>
          <div className="markdown">
            <ul>
              <li>{translate('regulatory_report.bullet_point1')}</li>
              <li>{translate('regulatory_report.bullet_point2')}</li>
              <li>{translate('regulatory_report.bullet_point3')}</li>
            </ul>
          </div>
          <p>{translate('regulatory_report.description2')}</p>
          <div className="modal-field big-spacer-top">
            <label htmlFor="regulatory-report-branch-select">
              {translate('regulatory_page.select_branch')}
            </label>
            <Select
              className="width-100"
              inputId="regulatory-report-branch-select"
              id="regulatory-report-branch-select-input"
              onChange={this.onBranchSelect}
              options={branchLikesOptions}
              value={branchLikesOptions.find((o) => o.value === selectedBranch)}
            />
          </div>
          <div className="modal-field big-spacer-top">
            {downloadStarted && (
              <div>
                <p>{translate('regulatory_page.download_start.sentence')}</p>
              </div>
            )}
          </div>
        </div>
        <div className="modal-foot">
          <a
            className={classNames('button button-primary big-spacer-right', {
              disabled: downloadStarted,
            })}
            download={[component.name, selectedBranch, 'regulatory report.zip']
              .filter((s) => !!s)
              .join(' - ')}
            onClick={() => this.setState({ downloadStarted: true })}
            href={getRegulatoryReportUrl(component.key, selectedBranch)}
            target="_blank"
            rel="noopener noreferrer"
          >
            {translate('download_verb')}
          </a>
          <ButtonLink onClick={onClose}>{translate('close')}</ButtonLink>
        </div>
      </>
    );
  }
}
