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
class Treemap2
  include ActionView::Helpers::UrlHelper

  attr_accessor :size_metric, :color_metric, :width, :height, :root_snapshot, :period_index,
                :id, :id_counter, :measures, :browsable

  def initialize(id, size_metric, width, height, options={})
    @id_counter = 0
    @id = id
    @size_metric = size_metric
    @width = width
    @height = height

    @color_metric = options[:color_metric]
    @root_snapshot = options[:root_snapshot]
    @measures = options[:measures] # pre-computed measures, for example by filters
    if options[:period_index] && options[:period_index]>0
      @period_index = options[:period_index]
    end
  end

  def generate_html
    root = Treemap::Node.new(:id => -1, :label => '')
    build_tree(root)

    output = Sonar::HtmlOutput.new do |o|
      o.width = @width
      o.height = @height
      o.full_html = false
      o.details_at_depth = 1
    end
    output.to_html(root)
  end

  def empty?
    @id_counter==0
  end

  protected

  def measures
    @measures ||=
      begin
        metric_ids=[@size_metric.id]
        metric_ids << @color_metric.id if @color_metric && @color_metric.id!=@size_metric.id

        sql_conditions = 'snapshots.islast=? AND project_measures.characteristic_id IS NULL and project_measures.rule_id IS NULL ' +
          'and project_measures.rule_priority IS NULL and project_measures.metric_id in (?)'
        sql_values = [true, metric_ids]
        if @root_snapshot
          sql_conditions += " AND snapshots.parent_snapshot_id=?"
          sql_values << @root_snapshot.id
        else
          sql_conditions<<" AND snapshots.scope='PRJ' and snapshots.qualifier='TRK'"
        end

        ProjectMeasure.find(:all, :include => {:snapshot => :project}, :conditions => [sql_conditions].concat(sql_values))
      end
  end

  def build_tree(node)
    color_measures_by_sid={}
    if @color_metric
      measures.each do |measure|
        color_measures_by_sid[measure.snapshot_id]=measure if measure.metric_id==@color_metric.id
      end
    end

    measures.each do |measure|
      if measure.metric_id==@size_metric.id
        color_measure = color_measures_by_sid[measure.snapshot_id]
        resource = measure.snapshot.project
        child = Treemap::Node.new(:id => "#{@id}-#{@id_counter += 1}",
                                  :size => size_value(measure),
                                  :label => resource.name(false),
                                  :title => escape_javascript(resource.name(true)),
                                  :tooltip => tooltip(resource, measure, color_measure),
                                  :color => html_color(color_measure),
                                  :rid => resource.id,
                                  :browsable => resource.display_dashboard?)
        node.add_child(child)
      end
    end
  end

  def tooltip(resource,size_measure, color_measure)
    html=CGI::escapeHTML(resource.name(true))
    html += " - #{CGI::escapeHTML(@size_metric.short_name)}: #{CGI::escapeHTML(size_measure.formatted_value)}"
    if color_measure
      html += " - #{CGI::escapeHTML(@color_metric.short_name)}: #{CGI::escapeHTML(color_measure.formatted_value)}"
    end
    html
  end

  def size_value(measure)
    if @period_index
      var=measure.variation(@period_index)
      var ? var.to_f.abs : 0.0
    elsif measure.value
      measure.value.to_f.abs||0.0
    else
      0.0
    end
  end

  def html_color(measure)
    MeasureColor.color(measure).html
  end

end

class Sonar::HtmlOutput < Treemap::HtmlOutput

  def draw_node(node)
    return "" if node.bounds.nil?

    html = ''
    html += "<div id=\"node-#{node.id}\" style=\""
    html += "overflow:hidden;position:absolute;"
    html += "left:#{node.bounds.x1}px; top:#{node.bounds.y1}px;"
    html += "width:#{node.bounds.width}px;height: #{node.bounds.height}px;"
    html += "background-color:#FFF;"
    html += "\" alt='#{node.tooltip}' title='#{node.tooltip}'>"
    html += "<div rid='#{node.rid}' id=\"tm-node-#{node.id}\" style='margin: 1px;background-color: #{node.color}; height: #{node.bounds.height-4}px;
border: 1px solid #{node.color};' "
    if node.browsable
      html += "b=1 "
    end
    if @details_at_depth==node.depth
      html += "onmouseover=\"this.style.borderColor='#444';\" onmouseout=\"this.style.borderColor='#{node.color}';\""
    end
    html += ' >'
    html += draw_node_body(node)

    if (!node.children.nil? && node.children.size > 0)
      node.children.each do |c|
        html += draw_node(c)
      end
    end
    html += "</div></div>"
  end

  def draw_label(node)
    label= "<a href='#' onclick='return openResource(#{node.rid})'>"
    label += node_label(node)
    label += "</a>"
    label
  end

  def draw_tooltips(node)
    ''
  end
end