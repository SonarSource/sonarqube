/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  deleteProfile,
  getQualityProfileBackupUrl,
  renameProfile,
  setDefaultProfile
} from '../../../api/quality-profiles';
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from '../../../components/controls/ActionsDropdown';
import Tooltip from '../../../components/controls/Tooltip';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { getRulesUrl } from '../../../helpers/urls';
import { Profile, ProfileActionModals } from '../types';
import { getProfileComparePath, getProfilePath, PROFILE_PATH } from '../utils';
import DeleteProfileForm from './DeleteProfileForm';
import ProfileModalForm from './ProfileModalForm';

interface Props {
  className?: string;
  fromList?: boolean;
  profile: Profile;
  router: Pick<Router, 'push' | 'replace'>;
  updateProfiles: () => Promise<void>;
}

interface State {
  loading: boolean;
  openModal?: ProfileActionModals;
}

export class ProfileActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCloseModal = () => {
    this.setState({ openModal: undefined });
  };

  handleCopyClick = () => {
    this.setState({ openModal: ProfileActionModals.Copy });
  };

  handleExtendClick = () => {
    this.setState({ openModal: ProfileActionModals.Extend });
  };

  handleRenameClick = () => {
    this.setState({ openModal: ProfileActionModals.Rename });
  };

  handleDeleteClick = () => {
    this.setState({ openModal: ProfileActionModals.Delete });
  };

  handleProfileCopy = async (name: string) => {
    this.setState({ loading: true });

    try {
      await copyProfile(this.props.profile.key, name);
      this.profileActionPerformed(name);
    } catch {
      this.profileActionError();
    }
  };

  handleProfileExtend = async (name: string) => {
    const { profile: parentProfile } = this.props;

    const data = {
      language: parentProfile.language,
      name
    };

    this.setState({ loading: true });

    try {
      const { profile: newProfile } = await createQualityProfile(data);
      await changeProfileParent(newProfile, parentProfile);
      this.profileActionPerformed(name);
    } catch {
      this.profileActionError();
    }
  };

  handleProfileRename = async (name: string) => {
    this.setState({ loading: true });

    try {
      await renameProfile(this.props.profile.key, name);
      this.profileActionPerformed(name);
    } catch {
      this.profileActionError();
    }
  };

  handleProfileDelete = async () => {
    this.setState({ loading: true });

    try {
      await deleteProfile(this.props.profile);

      if (this.mounted) {
        this.setState({ loading: false, openModal: undefined });
        this.props.router.replace(PROFILE_PATH);
        this.props.updateProfiles();
      }
    } catch {
      this.profileActionError();
    }
  };

  handleSetDefaultClick = () => {
    const { profile } = this.props;
    if (profile.activeRuleCount > 0) {
      setDefaultProfile(profile).then(this.props.updateProfiles, () => {
        /* noop */
      });
    }
  };

  profileActionPerformed = (name: string) => {
    const { profile, router } = this.props;
    if (this.mounted) {
      this.setState({ loading: false, openModal: undefined });
      this.props.updateProfiles().then(
        () => {
          router.push(getProfilePath(name, profile.language));
        },
        () => {
          /* noop */
        }
      );
    }
  };

  profileActionError = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    const { profile } = this.props;
    const { loading, openModal } = this.state;
    const { actions = {} } = profile;

    const backupUrl = `${getBaseUrl()}${getQualityProfileBackupUrl(profile)}`;

    const activateMoreUrl = getRulesUrl({
      qprofile: profile.key,
      activation: 'false'
    });

    const hasNoActiveRules = profile.activeRuleCount === 0;

    return (
      <>
        <ActionsDropdown className={this.props.className}>
          {actions.edit && (
            <ActionsDropdownItem
              className="it__quality-profiles__activate-more-rules"
              to={activateMoreUrl}>
              {translate('quality_profiles.activate_more_rules')}
            </ActionsDropdownItem>
          )}

          {!profile.isBuiltIn && (
            <ActionsDropdownItem
              className="it__quality-profiles__backup"
              download={`${profile.key}.xml`}
              to={backupUrl}>
              {translate('backup_verb')}
            </ActionsDropdownItem>
          )}

          <ActionsDropdownItem
            className="it__quality-profiles__compare"
            to={getProfileComparePath(profile.name, profile.language)}>
            {translate('compare')}
          </ActionsDropdownItem>

          {actions.copy && (
            <>
              <ActionsDropdownItem
                className="it__quality-profiles__copy"
                onClick={this.handleCopyClick}>
                {translate('copy')}
              </ActionsDropdownItem>

              <ActionsDropdownItem
                className="it__quality-profiles__extend"
                onClick={this.handleExtendClick}>
                {translate('extend')}
              </ActionsDropdownItem>
            </>
          )}

          {actions.edit && (
            <ActionsDropdownItem
              className="it__quality-profiles__rename"
              onClick={this.handleRenameClick}>
              {translate('rename')}
            </ActionsDropdownItem>
          )}

          {actions.setAsDefault &&
            (hasNoActiveRules ? (
              <li>
                <Tooltip
                  placement="left"
                  overlay={translate('quality_profiles.cannot_set_default_no_rules')}>
                  <span className="it__quality-profiles__set-as-default text-muted-2">
                    {translate('set_as_default')}
                  </span>
                </Tooltip>
              </li>
            ) : (
              <ActionsDropdownItem
                className="it__quality-profiles__set-as-default"
                onClick={this.handleSetDefaultClick}>
                {translate('set_as_default')}
              </ActionsDropdownItem>
            ))}

          {actions.delete && <ActionsDropdownDivider />}

          {actions.delete && (
            <ActionsDropdownItem
              className="it__quality-profiles__delete"
              destructive={true}
              onClick={this.handleDeleteClick}>
              {translate('delete')}
            </ActionsDropdownItem>
          )}
        </ActionsDropdown>

        {openModal === ProfileActionModals.Copy && (
          <ProfileModalForm
            btnLabelKey="copy"
            headerKey="quality_profiles.copy_x_title"
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileCopy}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Extend && (
          <ProfileModalForm
            btnLabelKey="extend"
            headerKey="quality_profiles.extend_x_title"
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileExtend}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Rename && (
          <ProfileModalForm
            btnLabelKey="rename"
            headerKey="quality_profiles.rename_x_title"
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileRename}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Delete && (
          <DeleteProfileForm
            loading={loading}
            onClose={this.handleCloseModal}
            onDelete={this.handleProfileDelete}
            profile={profile}
          />
        )}
      </>
    );
  }
}

export default withRouter(ProfileActions);
