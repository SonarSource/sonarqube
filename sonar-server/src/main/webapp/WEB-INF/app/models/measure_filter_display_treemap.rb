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
class MeasureFilterDisplayTreemap < MeasureFilterDisplay
  include ActionView::Helpers::UrlHelper

  KEY = :treemap

  attr_reader :height, :id, :size, :size_metric, :color_metric

  def initialize(filter, options)
    super(filter, options)

    @size_metric = Metric.by_key(@filter.criteria('tmSize')||'ncloc')
    @color_metric = Metric.by_key(@filter.criteria('tmColor')||'violations_density')
    @html_id = options[:html_id]
    @filter.metrics=([@size_metric, @color_metric].compact)
    @height = (@filter.criteria('tmHeight')||'600').to_i
    @id_count = 0
  end

  def html
    if filter.results
      root = Treemap::Node.new(:id => -1, :label => '')
      build_tree(root)

      output = Sonar::HtmlOutput.new do |o|
        # width in percents
        o.width = 100
        o.height = @height
        o.full_html = false
        o.details_at_depth = 1
      end
      html = output.to_html(root)
      html + "<script>treemapById(#{@html_id}).onLoaded(#{@filter.results.size});</script>"
    end
  end

  def empty?
    @filter.results.nil? || @filter.results.empty?
  end

  private

  def build_tree(node)
    if @filter.results
      @filter.results.each do |result|
        size_measure=result.measure(@size_metric)
        if size_measure
          color_measure=(@color_metric ? result.measure(@color_metric) : nil)
          resource = result.snapshot.resource
          child = Treemap::Node.new(:id => "#{@html_id}-#{@id_count += 1}",
                                    :size => size_value(size_measure),
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
    html += "left:#{node.bounds.x1}%; top:#{node.bounds.y1}px;"
    html += "width:#{node.bounds.width}%;height: #{node.bounds.height}px;"
    html += "background-color:#FFF;\">"
    html += "<div rid='#{node.rid}' id=\"tm-node-#{node.id}\" style='margin: 1px;background-color: #{node.color}; height: #{node.bounds.height-4}px;
border: 1px solid #{node.color};' alt=\"#{node.tooltip}\" title=\"#{node.tooltip}\""
    if node.leaf
      html += "l=1 "
    end
    html += ' >'
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