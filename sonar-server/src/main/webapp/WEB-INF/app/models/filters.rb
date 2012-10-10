#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
class Filters

  def self.execute(filter, authenticated_system, options={})
    filter_context = FilterContext.new(filter, options)

    filter_json={}

    #----- FILTER ON RESOURCES
    if filter.resource_id
      filter_json[:base]=filter.resource.key
    end

    if filter.favourites
      filter_json[:favourites]=true
    end

    date_criterion=filter.criterion('date')
    if date_criterion
      if date_criterion.operator=='<'
        filter_json[:beforeDays]=date_criterion.value.to_i
      else
        filter_json[:afterDays]=date_criterion.value.to_i
      end
    end

    key_criterion=filter.criterion('key')
    if key_criterion
      filter_json[:keyRegexp]=key_criterion.text_value
    end

    name_criterion=filter.criterion('name')
    if name_criterion
      filter_json[:name]=name_criterion.text_value
    end

    qualifier_criterion=filter.criterion('qualifier')
    if qualifier_criterion
      filter_json[:qualifiers]=qualifier_criterion.text_values
    end

    filter_json[:onBaseChildren]=filter.on_direct_children?

    language_criterion=filter.criterion('language')
    if language_criterion
      filter_json[:languages]=language_criterion.text_values
    end


    #----- FILTER ON MEASURES
    filter_json[:conditions]=filter.measure_criteria.map do |c|
      hash = {:metric => c.metric.key, :op => c.operator, :val => c.value}
      if c.variation
        hash[:period] = filter_context.period_index || -1
      end
      hash
    end


    #----- SORTED COLUMN
    if filter_context.sorted_column_id
      filter.sorted_column=filter_context.sorted_column_id
    end
    if filter.sorted_column
      filter_json[:sortField]=filter.sorted_column.family.upcase
      if filter.sorted_column.on_metric? && filter.sorted_column.metric
        filter_json[:sortMetric]=filter.sorted_column.metric.key
        if filter.sorted_column.variation
          filter_json[:sortPeriod]=filter_context.period_index || -1
        end
      end
    end


    #----- SORTING DIRECTION
    if filter.sorted_column
      if filter_context.ascending_sort.nil?
        filter_json[:sortAsc]=filter.sorted_column.ascending?
      else
        filter.sorted_column.ascending=filter_context.ascending_sort
        filter_json[:sortAsc]=filter.sorted_column.ascending?
      end

      if filter_context.ascending_sort
        filter.sorted_column.ascending=filter_context.ascending_sort
      end
      filter_json[:sortAsc]=filter.sorted_column.ascending?
    end

    #----- EXECUTION
    user=authenticated_system.current_user
    rows=Api::Utils.java_facade.executeMeasureFilter(filter_json.to_json, user ? user.id : nil)
    snapshot_ids=extract_snapshot_ids(rows, authenticated_system)

    has_security_exclusions=(snapshot_ids.size < rows.size)
    filter_context.process_results(snapshot_ids, has_security_exclusions)
    filter_context
  end

  private

  def self.extract_snapshot_ids(rows, authenticated_system)
    sids=[]
    project_ids=rows.map { |row| row.getResourceRootId() }.compact.uniq
    authorized_pids=authenticated_system.select_authorized(:user, project_ids)
    rows.each do |row|
      if authorized_pids.include?(row.getResourceRootId())
        sids<<row.getSnapshotId()
      end
    end
    sids
  end
end