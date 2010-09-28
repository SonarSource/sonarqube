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
module FiltersHelper

  def execute_filter(filter, user=nil, options={})
    java_filter=Java::OrgSonarServerFilters::Filter.new

    #----- FILTER ON RESOURCES
    if filter.resource_id
      snapshot=Snapshot.find(:first, :conditions => {:project_id => filter.resource_id, :islast => true})
      if snapshot
        java_filter.setPath(snapshot.root_snapshot_id, snapshot.id, snapshot.path, (snapshot.view? || snapshot.subview?))
      else
        java_filter.setPath(-1, -1, '', false)
      end
    end

    if filter.favourites
      java_filter.setFavouriteIds((user ? user.favourite_ids : []).to_java(:Integer))
    end

    date_criterion=filter.criterion('date')
    if date_criterion
      java_filter.setDateCriterion(date_criterion.operator, date_criterion.value.to_i)
    end

    key_criterion=filter.criterion('key')
    if key_criterion
      java_filter.setKeyRegexp(key_criterion.text_value)
    end

    name_criterion=filter.criterion('name')
    if name_criterion
      java_filter.setNameRegexp(name_criterion.text_value)
    end

    qualifier_criterion=filter.criterion('qualifier')
    if qualifier_criterion
      java_filter.setQualifiers(qualifier_criterion.text_values.to_java(:String))
    else
      java_filter.setQualifiers([].to_java(:String))
    end

    language_criterion=filter.criterion('language')
    if language_criterion
      java_filter.setLanguages(language_criterion.text_values.to_java :String)
    end


    #----- FILTER ON MEASURES
    filter.measure_criteria.each do |c|
      java_filter.createMeasureCriterionOnValue(c.metric.id, c.operator, c.value)
    end


    #----- SORTED COLUMN
    if options[:sort]
      filter.sorted_column=options[:sort].to_i
    end
    if filter.sorted_column.on_name?
      java_filter.setSortedByName()

    elsif filter.sorted_column.on_date?
      java_filter.setSortedByDate()

    elsif filter.sorted_column.on_version?
      java_filter.setSortedByVersion()

    elsif filter.sorted_column.on_metric? && filter.sorted_column.metric
      metric=filter.sorted_column.metric
      java_filter.setSortedMetricId(metric.id, metric.numeric?)
    end


    #----- SORTING DIRECTION
    if options[:asc]
      filter.sorted_column.ascending=(options[:asc]=='true')
    end
    java_filter.setAscendingSort(filter.sorted_column.ascending?)


    #----- EXECUTION
    java_result=java_facade.execute_filter(java_filter)
    snapshot_ids=extract_snapshot_ids(java_result.getRows())



    options[:snapshot_ids]=snapshot_ids
    options[:security_exclusions]=(snapshot_ids.size < java_result.size())
    FilterResult.new(filter, options)
  end

  def column_title(column, filter)
    html=nil
    if column.sortable?
      asc = (column.descending? || column.sort_direction.nil?)
      html=link_to h(column.display_name), url_for(:overwrite_params => {:asc => (!(column.ascending?)).to_s, :sort => column.id})
    else
      html=h(column.display_name)
    end
    if filter.sorted_column==column
      html << (column.ascending? ? image_tag("asc12.png") : image_tag("desc12.png"))
    end
    html
  end

  def column_align(column)
    (column.on_name? || column.on_key?) ? 'left' : 'right' 
  end

  def treemap_metrics(filter)
    metrics=filter.measure_columns.map{|col| col.metric}
    size_metric=(metrics.size>=1 ? metrics[0] : Metric.by_key('ncloc'))
    color_metric=(metrics.size>=2 ? metrics[1] : Metric.by_key('violations_density'))
    [size_metric, color_metric]
  end

  def viewable_filter?(filter)
    if logged_in?
      filter.shared || (filter.user==current_user)
    else
      filter.shared
    end
  end

  def editable_filter?(filter)
    if logged_in?
      (filter.user && filter.user==current_user) || (!filter.user && is_admin?)
    else
      false
    end
  end


  private

  def extract_snapshot_ids(sql_rows)
    sids=[]
    project_ids=sql_rows.map{|r| r[2] ? to_integer(r[2]) : to_integer(r[1])}.compact.uniq
    authorized_pids=select_authorized(:user, project_ids)
    sql_rows.each do |row|
      pid=(row[2] ? to_integer(row[2]) : to_integer(row[1]))
      if authorized_pids.include?(pid)
        sids<<to_integer(row[0])
      end
    end
    sids
  end

  def to_integer(obj)
    if obj.is_a?(Fixnum)
      obj
    else
      # java.math.BigDecimal
      obj.intValue()
    end
  end
end