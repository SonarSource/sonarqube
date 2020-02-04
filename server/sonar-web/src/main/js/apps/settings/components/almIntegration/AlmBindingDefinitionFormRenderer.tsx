/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Button, ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface AlmBindingDefinitionFormRendererProps {
  canSubmit: () => boolean;
  children: React.ReactNode;
  help?: React.ReactNode;
  onCancel?: () => void;
  onDelete?: () => void;
  onEdit?: () => void;
  onSubmit: () => void;
  loading: boolean;
  success: boolean;
}

export default function AlmBindingDefinitionFormRenderer(
  props: AlmBindingDefinitionFormRendererProps
) {
  const { children, help, loading, success } = props;

  return (
    <form
      className="views-form"
      data-test="settings__alm-form"
      onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        props.onSubmit();
      }}>
      <div className="display-flex-start">
        <div className="flex-1">
          {children}

          <div className="display-flex-center">
            {props.onEdit === undefined ? (
              <SubmitButton disabled={loading || !props.canSubmit()}>
                {translate('settings.almintegration.form.save')}
              </SubmitButton>
            ) : (
              <Button disabled={loading} onClick={props.onEdit}>
                {translate('edit')}
              </Button>
            )}
            {props.onDelete && (
              <Button
                className="button-red spacer-left"
                disabled={loading}
                onClick={props.onDelete}>
                {translate('delete')}
              </Button>
            )}
            {props.onCancel && (
              <ResetButtonLink className="spacer-left" onClick={props.onCancel}>
                {translate('cancel')}
              </ResetButtonLink>
            )}
            {loading && <DeferredSpinner className="spacer-left" />}
            {!loading && success && (
              <span className="text-success spacer-left">
                <AlertSuccessIcon className="spacer-right" />
                {translate('settings.state.saved')}
              </span>
            )}
          </div>
        </div>

        {help && (
          <Alert className="huge-spacer-left flex-1" variant="info">
            {help}
          </Alert>
        )}
      </div>
    </form>
  );
}
