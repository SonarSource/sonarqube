 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2008-2011 SonarSource
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
module MetricsHelper

  def domains(metrics, translate=false)
    metrics.map {|m| m.domain(translate)}.uniq.compact.sort    
  end

  def options_grouped_by_domain(metrics, selected_key='')
    metrics_per_domain={}
    metrics.each do |metric|
      domain=metric.domain(true) || ''
      metrics_per_domain[domain]||=[]
      metrics_per_domain[domain]<<metric
    end

    html=''
    metrics_per_domain.keys.sort.each do |domain|
      html += "<optgroup label=\"#{html_escape(domain)}\">"
      metrics_per_domain[domain].each do |m|
        selected_attr = " selected='selected'" if (m.key==selected_key || m.id==selected_key)
        html += "<option value='#{html_escape(m.key)}'#{selected_attr}>#{html_escape(m.short_name(true))}</option>"
      end
      html += '</optgroup>'
    end
    html
  end
end