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
module MeasuresHelper

  def list_column_html(filter, column)

    if column.sort?
      html = link_to(h(column.name), filter.url_params.merge({:controller => 'measures', :action => 'search', :asc => (!filter.sort_asc?).to_s, :sort => column.key}))
    else
      html=h(column.name)
    end
    #if column.variation
    #  html="<img src='#{ApplicationController.root_context}/images/trend-up.png'></img> #{html}"
    #end

    if filter.sort_key==column.key
      html << (filter.sort_asc? ? image_tag("asc12.png") : image_tag("desc12.png"))
    end
    "<th class='#{column.align}'>#{html}</th>"
  end

  def list_cell_html(column, result)
    if column.metric
      format_measure(result.measure(column.metric))
    elsif column.key=='name'
      "#{qualifier_icon(result.snapshot)} #{link_to(result.snapshot.resource.name(true), {:controller => 'dashboard', :id => result.snapshot.resource_id}, :title => result.snapshot.resource.key)}"
    elsif column.key=='short_name'
      "#{qualifier_icon(result.snapshot)} #{link_to(result.snapshot.resource.name(false), {:controller => 'dashboard', :id => result.snapshot.resource_id}, :title => result.snapshot.resource.key)}"
    elsif column.key=='date'
      human_short_date(result.snapshot.created_at)
    elsif column.key=='key'
      "<span class='small'>#{result.snapshot.resource.kee}</span>"
    elsif column.key=='description'
      h result.snapshot.resource.description
    elsif column.key=='version'
      h result.snapshot.version
    elsif column.key=='language'
      h result.snapshot.resource.language
    elsif column.key=='links' && result.links
      html = ''
      result.links.select { |link| link.href.start_with?('http') }.each do |link|
        html += link_to(image_tag(link.icon, :alt => link.name), link.href, :class => 'nolink', :popup => true) unless link.custom?
      end
      html
    end
  end
end
