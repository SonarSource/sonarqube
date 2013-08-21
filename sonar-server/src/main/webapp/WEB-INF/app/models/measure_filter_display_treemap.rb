#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
require 'set'
class MeasureFilterDisplayTreemap < MeasureFilterDisplay
  include ActionView::Helpers::UrlHelper

  KEY = :treemap
  PROPERTY_KEYS = Set.new([:tmSize, :tmColor, :tmHeight])
  MAX_RESULTS = 1000
  DEFAULT_HEIGHT_PERCENTS = 55
  attr_reader :id, :size, :size_metric, :color_metric, :height_percents

  def initialize(filter, options)
    super(filter, options)

    @size_metric = Metric.by_key(@filter.criteria(:tmSize)||'ncloc')
    @color_metric = Metric.by_key(@filter.criteria(:tmColor)||'violations_density')
    @height_percents = (@filter.criteria(:tmHeight) || DEFAULT_HEIGHT_PERCENTS).to_i
    @height_percents = DEFAULT_HEIGHT_PERCENTS if @height_percents<=0
    @filter.metrics=([@size_metric, @color_metric].compact)
    @id_count = 0

    filter.set_criteria_value(:pageSize, MAX_RESULTS)
    filter.set_criteria_value(:page, 1)
  end

  def html
    # SONAR-3524
    # If filter is empty, we return a empty result in order to be treated more easily
    if filter.rows && !filter.rows.empty?
      root = Treemap::Node.new(:id => -1, :label => '')
      build_tree(root)
      output = Sonar::HtmlOutput.new do |o|
        # width in percents
        o.width = 100
        o.height = 100
        o.full_html = false
        o.details_at_depth = 1
      end
      output.to_html(root)
    end
  end

  def empty?
    @filter.rows.nil? || @filter.rows.empty?
  end

  def url_params
    @filter.criteria.select { |k, v| PROPERTY_KEYS.include?(k.to_sym) }
  end

  private

  def build_tree(node)
    if @filter.rows && @size_metric
      @filter.rows.each do |row|
        size_measure=row.measure(@size_metric)
        if size_measure
          color_measure=(@color_metric ? row.measure(@color_metric) : nil)
          resource = row.snapshot.resource
          child = Treemap::Node.new(:size => size_value(size_measure),
                                    :label => resource.name(false),
                                    :title => escape_javascript(resource.name(true)),
                                    :tooltip => tooltip(resource, size_measure, color_measure),
                                    :color => html_color(color_measure),
                                    :rid => resource.id,
                                    :leaf => resource.source_code?)
          node.add_child(child)
        end
      end
    end
  end

  def tooltip(resource, size_measure, color_measure)
    html=CGI::escapeHTML(resource.name(true))
    html += " - #{CGI::escapeHTML(@size_metric.short_name)}: #{CGI::escapeHTML(size_measure.formatted_value)}"
    if color_measure
      html += " - #{CGI::escapeHTML(@color_metric.short_name)}: #{CGI::escapeHTML(color_measure.formatted_value)}"
    end
    html
  end

  def size_value(measure)
    if measure.value
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
    html += "<div style=\""
    html += "overflow:hidden;position:absolute;"
    html += "left:#{node.bounds.x1}%; top:#{node.bounds.y1}%;"
    html += "width:#{node.bounds.width}%;height: #{node.bounds.height}%;"
    html += "background-color:#FFF;\">"
    if node.rid
      html += "<div rid='#{node.rid}' id=\"tm-node-#{node.id}\" style='margin: 1px;background-color: #{node.color}; height: 100%;
  border: 1px solid #{node.color};' alt=\"#{node.tooltip}\" title=\"#{node.tooltip}\""
      if node.leaf
        html += "l=1 "
      end
      html += ' >'
    else
      html += '<div>'
    end
    html += draw_node_body(node)

    if (!node.children.nil? && node.children.size > 0)
      node.children.each do |c|
        html += draw_node(c)
      end
    end
    html + '</div></div>'
  end

  def draw_label(node)
    if node.leaf
      "<a onclick=\"window.open(this.href,'resource','height=800,width=900,scrollbars=1,resizable=1');return false;\" " +
        "href=\"#{ApplicationController.root_context}/resource/index/#{node.rid}\">#{node_label(node)}</a>"
    else
      "<a href='#{ApplicationController.root_context}/dashboard/index/#{node.rid}'>#{node_label(node)}</a>"
    end
  end
end