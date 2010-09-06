#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class ReplaceNcssMetricByNclocMetric < ActiveRecord::Migration

  def self.up
    ncloc = Metric.find(:first, :conditions => {:name => 'ncss'})
    if ncloc
      ncloc = rename_ncss_to_ncloc(ncloc)
      old_ncss = create_old_ncss_from_ncloc(ncloc)
      copy_project_and_dir_ncloc_measures_to_ncss_measures(ncloc, old_ncss)
    end
  end

  def self.down
  end

  def self.rename_ncss_to_ncloc(ncss)
    ncss.name = 'ncloc'
    ncss.save!
    ncss
  end

  def self.create_old_ncss_from_ncloc(ncloc)
    old_ncss = Metric.new
    old_ncss.description = ncloc.description
    old_ncss.direction = ncloc.direction
    old_ncss.domain = ncloc.domain
    old_ncss.short_name = ncloc.short_name
    old_ncss.qualitative = ncloc.qualitative
    old_ncss.val_type = ncloc.val_type
    old_ncss.user_managed = ncloc.user_managed
    old_ncss.enabled = ncloc.enabled
    old_ncss.origin = ncloc.origin

    old_ncss.name = 'ncss'
    old_ncss.save!
    old_ncss
  end

  def self.copy_project_and_dir_ncloc_measures_to_ncss_measures(ncloc, ncss)
    ProjectMeasure.find(:all, :include => :snapshot,
      :conditions => ["metric_id=? AND snapshots.scope IN (?)", ncloc.id, ['PRJ','DIR']]).each do |measure|
      new_measure = ProjectMeasure.new
      new_measure.value = measure.value
      new_measure.snapshot_id = measure.snapshot_id
      new_measure.rule_id = measure.rule_id
      new_measure.rules_category_id = measure.rules_category_id
      new_measure.text_value = measure.text_value
      new_measure.tendency = measure.tendency
      new_measure.measure_date = measure.measure_date
      new_measure.project_id = measure.project_id
      new_measure.alert_status = measure.alert_status
      new_measure.alert_text = measure.alert_text
      new_measure.url = measure.url
      new_measure.description = measure.description

      new_measure.metric = ncss
      new_measure.save(false)
    end
  end
end