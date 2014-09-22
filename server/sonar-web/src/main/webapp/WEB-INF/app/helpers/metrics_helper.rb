 #
 # SonarQube, open source software quality management tool.
 # Copyright (C) 2008-2014 SonarSource
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
module MetricsHelper

  def domains(metrics, translate=false)
    metrics.map {|m| m.domain(translate)}.uniq.compact.sort    
  end

  def options_grouped_by_domain(metrics, selected_key='', options={})
    metrics_per_domain={}
    metrics.each do |metric|
      domain=metric.domain || ''
      metrics_per_domain[domain]||=[]
      metrics_per_domain[domain]<<metric
    end

    html=''
    if options[:include_empty]==true
      html+= "<option value=''></option>"
    end

    metrics_per_domain.keys.sort.each do |domain|
      html += "<optgroup label=\"#{html_escape(domain)}\">"
      metrics_per_domain[domain].each do |m|
        selected_attr = (m.key==selected_key || m.id==selected_key) ? " selected='selected'" : ''
        html += "<option value='#{html_escape(m.key)}'#{selected_attr}>#{html_escape(m.short_name)}</option>"
      end
      html += '</optgroup>'
    end
    html
  end
end
