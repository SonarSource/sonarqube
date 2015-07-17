#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

#
# Sonar 3.7.1
# SONAR-4633
#
class FixOracleTriggerNames < ActiveRecord::Migration

  def self.up
    if dialect()=='oracle'
      # sonar 3.7 creates triggers with names longer than max allowed (30 characters)
      # Drop them and re-create them with shorter names.
      # The triggers active_rule_param_changes_id_trg, characteristic_properties_id_trg and measure_filter_favourites_id_trg
      # are not supposed to exist.
      drop_trigger_quietly('action_plans_id_trg')
      drop_trigger_quietly('active_dashboards_id_trg')
      drop_trigger_quietly('active_rule_changes_id_trg')
      drop_trigger_quietly('active_rule_notes_id_trg')
      drop_trigger_quietly('active_rule_parameters_id_trg')
      drop_trigger_quietly('active_rules_id_trg')
      drop_trigger_quietly('authors_id_trg')
      drop_trigger_quietly('characteristics_id_trg')
      drop_trigger_quietly('dashboards_id_trg')
      drop_trigger_quietly('dependencies_id_trg')
      drop_trigger_quietly('duplications_index_id_trg')
      drop_trigger_quietly('events_id_trg')
      drop_trigger_quietly('graphs_id_trg')
      drop_trigger_quietly('group_roles_id_trg')
      drop_trigger_quietly('groups_id_trg')
      drop_trigger_quietly('issue_changes_id_trg')
      drop_trigger_quietly('issues_id_trg')
      drop_trigger_quietly('loaded_templates_id_trg')
      drop_trigger_quietly('manual_measures_id_trg')
      drop_trigger_quietly('measure_data_id_trg')
      drop_trigger_quietly('measure_filters_id_trg')
      drop_trigger_quietly('metrics_id_trg')
      drop_trigger_quietly('notifications_id_trg')
      drop_trigger_quietly('project_links_id_trg')
      drop_trigger_quietly('project_measures_id_trg')
      drop_trigger_quietly('projects_id_trg')
      drop_trigger_quietly('properties_id_trg')
      drop_trigger_quietly('quality_models_id_trg')
      drop_trigger_quietly('resource_index_id_trg')
      drop_trigger_quietly('rule_notes_id_trg')
      drop_trigger_quietly('rules_id_trg')
      drop_trigger_quietly('rules_parameters_id_trg')
      drop_trigger_quietly('rules_profiles_id_trg')
      drop_trigger_quietly('semaphores_id_trg')
      drop_trigger_quietly('snapshot_data_id_trg')
      drop_trigger_quietly('snapshot_sources_id_trg')
      drop_trigger_quietly('snapshots_id_trg')
      drop_trigger_quietly('user_roles_id_trg')
      drop_trigger_quietly('users_id_trg')
      drop_trigger_quietly('widget_properties_id_trg')
      drop_trigger_quietly('widgets_id_trg')

      create_id_trigger('action_plans')
      create_id_trigger('active_dashboards')
      create_id_trigger('active_rule_changes')
      create_id_trigger('active_rule_notes')
      create_id_trigger('active_rule_param_changes')
      create_id_trigger('active_rule_parameters')
      create_id_trigger('active_rules')
      create_id_trigger('authors')
      create_id_trigger('characteristic_properties')
      create_id_trigger('characteristics')
      create_id_trigger('dashboards')
      create_id_trigger('dependencies')
      create_id_trigger('duplications_index')
      create_id_trigger('events')
      create_id_trigger('graphs')
      create_id_trigger('group_roles')
      create_id_trigger('groups')
      create_id_trigger('issue_changes')
      create_id_trigger('issues')
      create_id_trigger('loaded_templates')
      create_id_trigger('manual_measures')
      create_id_trigger('measure_data')
      create_id_trigger('measure_filter_favourites')
      create_id_trigger('measure_filters')
      create_id_trigger('metrics')
      create_id_trigger('notifications')
      create_id_trigger('project_links')
      create_id_trigger('project_measures')
      create_id_trigger('projects')
      create_id_trigger('properties')
      create_id_trigger('quality_models')
      create_id_trigger('resource_index')
      create_id_trigger('rule_notes')
      create_id_trigger('rules')
      create_id_trigger('rules_parameters')
      create_id_trigger('rules_profiles')
      create_id_trigger('semaphores')
      create_id_trigger('snapshot_data')
      create_id_trigger('snapshot_sources')
      create_id_trigger('snapshots')
      create_id_trigger('user_roles')
      create_id_trigger('users')
      create_id_trigger('widget_properties')
      create_id_trigger('widgets')
    end
  end

  def self.drop_trigger_quietly(trigger_name)
    begin
      drop_trigger(trigger_name)
    rescue
      # name is too long, trigger does not exist, ...
    end
  end
end

