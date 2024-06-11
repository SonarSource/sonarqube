## Purpose

This document presents ownership of the tables in the Core database. This is intended
to facilitate the understanding which squad is responsible for which data.

Some tables have no ownership assigned yet, this is subject to change pending discussion between impacted squads.

It is expected to edit this file if the ownership of a table changes. There is no automated process to validate it, at the
same time we expect no frequent changes.

Important read: [Data Ownership Principles](https://xtranet-sonarsource.atlassian.net/wiki/spaces/DEV/pages/3170795538/Data+Ownership+Principle)

## Table Ownership

| Table | Owning Squad |
| --- | --- |
| active_rule_parameters | Dev and Team Workflow Squad |
| active_rules | Dev and Team Workflow Squad |
| alm_pats | Integration Squad |
| alm_settings | Integration Squad |
| analysis_properties | Analysis Experience Squad |
| anticipated_transitions |  |
| app_branch_project_branch |  |
| app_projects |  |
| audits |  |
| ce_activity | Analysis Experience Squad |
| ce_queue | Analysis Experience Squad |
| ce_scanner_context | Analysis Experience Squad |
| ce_task_characteristics | Analysis Experience Squad |
| ce_task_input | Analysis Experience Squad |
| ce_task_message | Analysis Experience Squad |
| components |  |
| default_qprofiles |  |
| deprecated_rule_keys |  |
| duplications_index |  |
| es_queue |  |
| event_component_changes |  |
| events |  |
| external_groups |  |
| file_sources | Analysis Experience Squad |
| github_orgs_groups |  |
| github_perms_mapping |  |
| group_roles |  |
| groups | Identity Squad |
| groups_users | Identity Squad |
| internal_component_props |  |
| internal_properties |  |
| issue_changes | Analysis Experience Squad |
| issues | Analysis Experience Squad |
| issues_fixed | Analysis Experience Squad |
| issues_impacts | Analysis Experience Squad |
| live_measures | Analysis Experience Squad |
| metrics |  |
| new_code_periods | Dev and Team Workflow Squad |
| new_code_reference_issues |  |
| notifications | Dev and Team Workflow Squad |
| org_qprofiles | Dev and Team Workflow Squad |
| perm_templates_groups |  |
| perm_templates_users |  |
| perm_tpl_characteristics |  |
| permission_templates |  |
| plugins |  |
| portfolio_proj_branches | Enterprise Reporting and Hierarchy Squad |
| portfolio_projects | Enterprise Reporting and Hierarchy Squad |
| portfolio_references | Enterprise Reporting and Hierarchy Squad |
| portfolios | Enterprise Reporting and Hierarchy Squad |
| project_alm_settings | Integration Squad |
| project_badge_token | Dev and Team Workflow Squad |
| project_branches |  |
| project_links | Dev and Team Workflow Squad |
| project_measures |  |
| project_qgates | Dev and Team Workflow Squad |
| project_qprofiles | Dev and Team Workflow Squad |
| projects |  |
| properties |  |
| push_events | Sonar Solution Squad |
| qgate_group_permissions | Dev and Team Workflow Squad |
| qgate_user_permissions | Dev and Team Workflow Squad |
| qprofile_changes | Dev and Team Workflow Squad |
| qprofile_edit_groups | Dev and Team Workflow Squad |
| qprofile_edit_users | Dev and Team Workflow Squad |
| quality_gate_conditions | Dev and Team Workflow Squad |
| quality_gates | Dev and Team Workflow Squad |
| report_schedules |  |
| report_subscriptions |  |
| rule_changes | Analysis Experience Squad |
| rule_desc_sections | Analysis Experience Squad |
| rule_impact_changes |  |
| rule_repositories | Analysis Experience Squad |
| rule_tags |  |
| rules | Analysis Experience Squad |
| rules_default_impacts |  |
| rules_parameters |  |
| rules_profiles | Dev and Team Workflow Squad |
| saml_message_ids |  |
| scanner_analysis_cache |  |
| schema_migrations |  |
| scim_groups |  |
| scim_users |  |
| scm_accounts |  |
| session_tokens | Identity Squad |
| snapshots | Analysis Experience Squad |
| user_dismissed_messages | Dev and Team Workflow Squad |
| user_roles |  |
| user_tokens | Identity Squad |
| users | Identity Squad |
| webhook_deliveries | Dev and Team Workflow Squad |
| webhooks | Dev and Team Workflow Squad |
