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
class TrendsChart

  def self.png_chart(width, height, resource, metrics, snapshots_id, locale, display_legend)
    java_chart = Java::OrgSonarServerChartsJruby::TrendsChart.new(width, height, locale.gsub(/\-/, '_'), display_legend)

    init_series(java_chart, metrics)
    metric_ids=metrics.map{|m| m.id}
    add_measures(java_chart, time_machine_measures(resource, metric_ids))
    add_measures(java_chart, time_machine_reviews(resource, metric_ids))
    add_labels(java_chart, resource);

    export_chart_as_png(java_chart)
  end


  protected

  def self.init_series(java_chart, metrics)
    metrics.each do |metric|
      java_chart.initSerie(metric.id, metric.short_name, metric.val_type==Metric::VALUE_TYPE_PERCENT)
    end
  end

  def self.time_machine_measures(resource, metric_ids)
    unless metric_ids.empty?
      sql = Project.send(:sanitize_sql, [
          "select s.created_at as created_at, m.value as value, m.metric_id as metric_id " +
            " from project_measures m LEFT OUTER JOIN snapshots s ON s.id=m.snapshot_id " +
            " where m.rule_id is null " +
            " and s.status='%s' " +
            " and s.project_id=%s " +
            " and m.metric_id in (%s) " + 
            " and m.rules_category_id is null and m.rule_priority is null and m.characteristic_id is null", Snapshot::STATUS_PROCESSED, resource.id, metric_ids * ','] )
      ProjectMeasure.connection.select_all( sql )
    end
  end
  
  def self.time_machine_reviews(resource, metric_ids)
    unless metric_ids.empty?
      sql = Project.send(:sanitize_sql, [
          "SELECT m.measure_date as created_at, m.value as value, m.metric_id as metric_id " +
            "FROM project_measures m " +
            "WHERE m.snapshot_id IS NULL " +
            "AND m.rule_id is null " +
            "AND m.project_id=%s " +
            "AND m.metric_id in (%s) " + 
            "AND m.rules_category_id is null and m.rule_priority is null and m.characteristic_id IS NULL", resource.id, metric_ids * ','] )
      ProjectMeasure.connection.select_all( sql )
    end    
  end  
  
  def self.add_measures(java_chart, sqlresult)
    if sqlresult && sqlresult.size>0
      sqlresult.each do |hash|
        value = hash["value"].to_f
        time = Time.parse( hash["created_at"].to_s )
        serie_id=hash["metric_id"].to_i
        java_chart.addMeasure(value, time, serie_id)
      end
    end
  end
  
  def self.add_labels(java_chart, resource)
    categories=EventCategory.categories(true)
    category_names=categories.map{|cat| cat.name}
    Event.find(:all, :conditions => {:resource_id => resource.id}).each do |event|
      if category_names.include?(event.category)
        time = Time.parse( event.event_date.to_s )
        java_chart.addLabel(time, event.name, event.category==EventCategory::KEY_ALERT)
      end
    end
  end
  
  def self.export_chart_as_png(java_chart)
    java_int_array_to_bytes(java_chart.exportChartAsPNG())
  end

  def self.java_int_array_to_bytes( java_int_array )
    # pack does not exists on the return java array proxy object
    # the java proxy array contains integers, they must be packed
    # to return a correct byte array stream (c*)
    [].concat(java_int_array).pack("c*")
  end

end