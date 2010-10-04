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
class Sonar::Treemap 
  include ActionView::Helpers::UrlHelper
  
  attr_accessor :color_metric, :size_metric, :width, :height
  
  def initialize(measures_by_snapshot, width, height, size_metric, color_metric)
    @measures_by_snapshot = measures_by_snapshot
    @size_metric = size_metric
    @color_metric = color_metric
    @width = width
    @height = height
        
    @min = 0
    @max = 100
    @min_color = Color::RGB.from_html("FF0000")   # red
    @mean_color = Color::RGB.from_html("FFB000")   # orange
    @max_color = Color::RGB.from_html("00FF00")   # green
  end
  
  def generate_html
    root = Treemap::Node.new( :id => -1, :label => '' )
    build_tree(root)
    
    output = Sonar::HtmlOutput.new do |o|
      o.width = @width
      o.height = @height
      o.full_html = false
      o.details_at_depth = 1
    end
    output.to_html(root)
  end
  
  
  protected

  def build_tree( node )
    id_counter = 0
    @measures_by_snapshot.each_pair do |snapshot, measures|
      size_measure=measures[@size_metric]
      color_measure=measures[@color_metric]

      if size_measure
        resource = snapshot.project
        child = Treemap::Node.new( :id => (id_counter += 1), 
          :size => size_measure.value.to_f||0, 
          :label => resource.name(false),
          :title => escape_javascript(resource.name(true)),
          :tooltip => get_html_tooltip(snapshot, size_measure, color_measure),
          :color => get_hex_color(color_measure),
          :url => get_url(snapshot,color_measure))
        node.add_child(child)
      end
    end
  end 

  
  def get_url(snapshot,color_measure)
    if snapshot.display_dashboard?
      "document.location='#{ApplicationController.root_context}/project/index/#{snapshot.project.copy_resource_id || snapshot.project_id}'"
    else
      "window.open('#{ApplicationController.root_context}/resource/index/#{snapshot.project_id}?viewer_metric_key=#{@color_metric.key}','resource','height=800,width=900,scrollbars=1,resizable=1');return false;"
    end 
  end
    
  def get_html_tooltip(snapshot, size_measure, color_measure)
    html = "<table>"
    html += "<tr><td align=left>#{escape_javascript(@size_metric.short_name)}</td><td align=right><b>#{escape_javascript(size_measure ? size_measure.formatted_value : '-')}</b></td></tr>"
    html += "<tr><td align=left>#{escape_javascript(@color_metric.short_name)}</td><td align=right><b>#{escape_javascript(color_measure ? color_measure.formatted_value : '-')}</b></td></tr>"
    html += "</table>"
    html
  end
  
  def get_color(color_measure)
    value=percentage_value(color_measure)
    if value<0
      return Color::RGB.from_html("DDDDDD")
    end

    interval = (@max - @min)/2
    mean = (@min + @max) / 2.0
    if (value > mean)
      value_percent = ((value - mean) / interval) * 100.0
      color = @max_color.mix_with(@mean_color, value_percent)
    else
      value_percent = ((mean - value) / interval) * 100.0
      color = @min_color.mix_with(@mean_color, value_percent)
    end
    color
  end

  def percentage_value(color_measure)
    return -1 if color_measure.nil?

    metric=color_measure.metric
    value=-1
    if !color_measure.alert_status.blank?
      case(color_measure.alert_status)
        when Metric::TYPE_LEVEL_OK : value=100
        when Metric::TYPE_LEVEL_ERROR : value=0
        when Metric::TYPE_LEVEL_WARN : value=50
      end

    elsif metric.value_type==Metric::VALUE_TYPE_LEVEL
      case(color_measure.text_value)
        when Metric::TYPE_LEVEL_OK : value=100
        when Metric::TYPE_LEVEL_WARN : value=50
        when Metric::TYPE_LEVEL_ERROR : value=0
      end
    elsif metric.worst_value && metric.best_value
      range=metric.best_value.to_f - metric.worst_value.to_f
      value = (color_measure.value.to_f - metric.worst_value.to_f) * (100.0 / range)
      value=100 if value>100.0
      value=0 if value<0.0
    end
    value
  end

  def get_hex_color(color_measure)
    get_color(color_measure).html
  end
end

class Sonar::HtmlOutput < Treemap::HtmlOutput
  attr_accessor(:link_url)
  
  def draw_node(node)
    return "" if node.bounds.nil?

    html = ""
    html += "<div id=\"node-#{node.id}\" style=\""
    html += "overflow: hidden; position:absolute;"
    html += "left: #{node.bounds.x1}px; top: #{node.bounds.y1}px;"
    html += "width: #{node.bounds.width}px; height: #{node.bounds.height}px;"
    html += "background-color: #FFF;"
    html += "\" class=\"node\">"
    html += "<div id=\"link_node-#{node.id}\" style='margin: 2px;background-color: #{node.color}; height: #{node.bounds.height-4}px; border: 1px solid #{node.color};' "
    if node.url && @details_at_depth==node.depth
      html += "onClick=\"#{node.url}\" onmouseover=\"this.style.borderColor='#111';\" onmouseout=\"this.style.borderColor='#{node.color}';\""
    end
    html += ' >'
    html += draw_node_body(node)

    if(!node.children.nil? && node.children.size > 0)
      node.children.each do |c|
        html += draw_node(c)
      end
    end
    html += "</div></div>"
  end
   
  def draw_tooltips(node)
    html='<script type="text/javascript">'
    
    node.children.each do |c|
      if @details_at_depth==c.depth
        html += "new Tip($('node-#{c.id.to_s}'), '#{c.tooltip}', {title: '#{c.title}'});"
      end
    end
    html += '</script>'
    html
  end
end