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
class Sonar::Treemap 
  include ActionView::Helpers::UrlHelper
  
  attr_accessor :color_metric, :size_metric, :width, :height
  
  def initialize(measures_by_snapshot, width, height, size_metric, color_metric, options={})
    @measures_by_snapshot = measures_by_snapshot
    @size_metric = size_metric
    @color_metric = color_metric if color_metric && color_metric.treemap_color?
    @width = width
    @height = height

    if options[:period_index] && options[:period_index]>0
      @period_index = options[:period_index]
    end
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
      color_measure=measures[@color_metric] if @color_metric

      if size_measure
        resource = snapshot.project
        child = Treemap::Node.new( :id => (id_counter += 1), 
          :size => size_value(size_measure),
          :label => resource.name(false),
          :title => escape_javascript(resource.name(true)),
          :tooltip => get_html_tooltip(snapshot, size_measure, color_measure),
          :color => html_color(color_measure),
          :url => get_url(snapshot))
        node.add_child(child)
      end
    end
  end 

  
  def get_url(snapshot)
    if snapshot.display_dashboard?
      "document.location='#{ApplicationController.root_context}/dashboard/index/#{snapshot.project.copy_resource_id || snapshot.project_id}'"
    else
      color_metric_key=(@color_metric ? @color_metric.key : nil)
      "window.open('#{ApplicationController.root_context}/resource/index/#{snapshot.project_id}?metric=#{color_metric_key}','resource','height=800,width=900,scrollbars=1,resizable=1');return false;"
    end 
  end
    
  def get_html_tooltip(snapshot, size_measure, color_measure)
    html = "<table>"
    html += "<tr><td align=left>#{escape_javascript(@size_metric.short_name)}</td><td align=right><b>#{escape_javascript(size_measure ? size_measure.formatted_value : '-')}</b></td></tr>"
    if color_measure
      html += "<tr><td align=left>#{escape_javascript(@color_metric.short_name)}</td><td align=right><b>#{escape_javascript(color_measure ? color_measure.formatted_value : '-')}</b></td></tr>"
    end
    html += "</table>"
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
  attr_accessor(:link_url)
  
  def draw_node(node)
    return "" if node.bounds.nil?

    html = ""
    html += "<div id=\"node-#{node.id}\" style=\""
    html += "overflow:hidden;position:absolute;"
    html += "left:#{node.bounds.x1}px; top:#{node.bounds.y1}px;"
    html += "width:#{node.bounds.width}px;height: #{node.bounds.height}px;"
    html += "background-color:#FFF;"
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