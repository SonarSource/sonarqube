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
import {
  ActionsDropdown,
  ItemButton,
  ItemDangerButton,
  ItemDivider,
  ItemDownload,
  ItemLink,
  PopupPlacement,
  PopupZLevel,
  Tooltip,
} from 'design-system';
import { some } from 'lodash';
import * as React from 'react';
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  deleteProfile,
  renameProfile,
  setDefaultProfile,
} from '../../../api/quality-profiles';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { getRulesUrl } from '../../../helpers/urls';
import { PROFILE_PATH } from '../constants';
import { Profile, ProfileActionModals } from '../types';
import { getProfileComparePath, getProfilePath } from '../utils';
import DeleteProfileForm from './DeleteProfileForm';
import ProfileModalForm from './ProfileModalForm';

interface Props {
  profile: Profile;
  router: Router;
  isComparable: boolean;
  updateProfiles: () => Promise<void>;
}

interface State {
  loading: boolean;
  openModal?: ProfileActionModals;
}

class ProfileActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: false,
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
      name,
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
        },
      );
    }
  };

  profileActionError = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  getQualityProfileBackupUrl = ({ language, name: qualityProfile }: Profile) => {
    const queryParams = Object.entries({ language, qualityProfile })
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join('&');
    return `/api/qualityprofiles/backup?${queryParams}`;
  };

  render() {
    const { profile, isComparable } = this.props;
    const { loading, openModal } = this.state;
    const { actions = {} } = profile;

    const backupUrl = `${getBaseUrl()}${this.getQualityProfileBackupUrl(profile)}`;

    const activateMoreUrl = getRulesUrl({
      qprofile: profile.key,
      activation: 'false',
    });

    const hasNoActiveRules = profile.activeRuleCount === 0;
    const hasAnyAction = some([...Object.values(actions), !profile.isBuiltIn, isComparable]);

    if (!hasAnyAction) {
      return null;
    }

    return (
      <>
        <ActionsDropdown
          allowResizing
          id={`quality-profile-actions-${profile.key}`}
          className="it__quality-profiles__actions-dropdown"
          toggleClassName="it__quality-profiles__actions-dropdown-toggle"
          ariaLabel={translateWithParameters(
            'quality_profiles.actions',
            profile.name,
            profile.languageName,
          )}
          zLevel={PopupZLevel.Global}
        >
          {actions.edit && (
            <ItemLink className="it__quality-profiles__activate-more-rules" to={activateMoreUrl}>
              {translate('quality_profiles.activate_more_rules')}
            </ItemLink>
          )}

          {!profile.isBuiltIn && (
            <ItemDownload
              download={`${profile.key}.xml`}
              href={backupUrl}
              className="it__quality-profiles__backup"
            >
              {translate('backup_verb')}
            </ItemDownload>
          )}

          {isComparable && (
            <ItemLink
              className="it__quality-profiles__compare"
              to={getProfileComparePath(profile.name, profile.language)}
            >
              {translate('compare')}
            </ItemLink>
          )}

          {actions.copy && (
            <>
              <Tooltip
                overlay={translateWithParameters('quality_profiles.extend_help', profile.name)}
                placement={PopupPlacement.Left}
              >
                <ItemButton
                  className="it__quality-profiles__extend"
                  onClick={this.handleExtendClick}
                >
                  {translate('extend')}
                </ItemButton>
              </Tooltip>

              <Tooltip
                overlay={translateWithParameters('quality_profiles.copy_help', profile.name)}
                placement={PopupPlacement.Left}
              >
                <ItemButton className="it__quality-profiles__copy" onClick={this.handleCopyClick}>
                  {translate('copy')}
                </ItemButton>
              </Tooltip>
            </>
          )}

          {actions.edit && (
            <ItemButton className="it__quality-profiles__rename" onClick={this.handleRenameClick}>
              {translate('rename')}
            </ItemButton>
          )}

          {actions.setAsDefault &&
            (hasNoActiveRules ? (
              <li>
                <Tooltip
                  placement={PopupPlacement.Left}
                  overlay={translate('quality_profiles.cannot_set_default_no_rules')}
                >
                  <ItemButton
                    className="it__quality-profiles__set-as-default"
                    onClick={this.handleSetDefaultClick}
                    disabled
                  >
                    {translate('set_as_default')}
                  </ItemButton>
                </Tooltip>
              </li>
            ) : (
              <ItemButton
                className="it__quality-profiles__set-as-default"
                onClick={this.handleSetDefaultClick}
              >
                {translate('set_as_default')}
              </ItemButton>
            ))}

          {actions.delete && (
            <>
              <ItemDivider />
              <ItemDangerButton
                className="it__quality-profiles__delete"
                onClick={this.handleDeleteClick}
              >
                {translate('delete')}
              </ItemDangerButton>
            </>
          )}
        </ActionsDropdown>

        {openModal === ProfileActionModals.Copy && (
          <ProfileModalForm
            action={openModal}
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileCopy}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Extend && (
          <ProfileModalForm
            action={openModal}
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileExtend}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Rename && (
          <ProfileModalForm
            action={openModal}
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
