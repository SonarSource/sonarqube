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
import { ButtonPrimary, FlagMessage, InputSelect, Modal } from 'design-system';
import * as React from 'react';
import { setWorkerCount } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';

const MAX_WORKERS = 10;
const WORKERS_FORM_ID = 'workers-form';

interface Props {
  onClose: (newWorkerCount?: number) => void;
  workerCount: number;
}

interface State {
  newWorkerCount: number;
  submitting: boolean;
}

export default class WorkersForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      newWorkerCount: props.workerCount,
      submitting: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClose = () => {
    this.props.onClose();
  };

  handleWorkerCountChange = (option: { label: string; value: number }) =>
    this.setState({ newWorkerCount: option.value });

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    const { newWorkerCount } = this.state;
    setWorkerCount(newWorkerCount).then(
      () => {
        if (this.mounted) {
          this.props.onClose(newWorkerCount);
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ submitting: false });
        }
      },
    );
  };

  render() {
    const { newWorkerCount, submitting } = this.state;

    const options = [];
    for (let i = 1; i <= MAX_WORKERS; i++) {
      options.push({ label: String(i), value: i });
    }

    return (
      <Modal
        headerTitle={translate('background_tasks.change_number_of_workers')}
        onClose={this.handleClose}
        isOverflowVisible
        body={
          <form id={WORKERS_FORM_ID} onSubmit={this.handleSubmit}>
            <InputSelect
              aria-label={translate('background_tasks.change_number_of_workers')}
              className="sw-mt-2"
              isSearchable={false}
              onChange={this.handleWorkerCountChange}
              options={options}
              size="medium"
              value={options.find((o) => o.value === newWorkerCount)}
            />
            <FlagMessage className="sw-mt-4" variant="info">
              {translate('background_tasks.change_number_of_workers.hint')}
            </FlagMessage>
          </form>
        }
        primaryButton={
          <ButtonPrimary disabled={submitting} type="submit" form={WORKERS_FORM_ID}>
            {translate('save')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
        loading={submitting}
      />
    );
  }
}
