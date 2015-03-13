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
class ProjectLink < ActiveRecord::Base
  belongs_to :project, :foreign_key => 'component_uuid', :primary_key => 'uuid'
  
  LINK_HOMEPAGE = "homepage"
  LINK_CONTINUOUS_INTEGRATION = "ci"
  LINK_ISSUE_TRACKER = "issue"
  LINK_SCM_URL = "scm"
  LINK_SCM_RO_CONNECTION = "scm_ro"
  LINK_SCM_DEV_CONNECTION = "scm_dev"

  CORE_LINK_KEYS = [LINK_HOMEPAGE,LINK_CONTINUOUS_INTEGRATION,LINK_ISSUE_TRACKER,LINK_SCM_URL,LINK_SCM_DEV_CONNECTION]
  
 def custom?
    !CORE_LINK_KEYS.include?(key)
  end

  def type
    link_type
  end

  def key
    link_type
  end

  def name(translate=true)
    default_string = read_attribute(:name)
    return default_string unless translate
    
    i18n_key = 'project_links.' + read_attribute(:link_type)
    Api::Utils.message(i18n_key, :default => default_string)
  end

  def self.name_to_key(s)
    s.tr(' ', '_')[0..19]
  end

  def icon
    if custom?
      'links/external.png'
    else
      "links/#{key}.png"
    end
  end

  def to_hash_json
    {'type' => link_type, 'name' => name, 'url' => href}
  end

  def to_xml(xml)
    xml.link do
      xml.type(link_type)
      xml.name(name)
      xml.url(href)
    end
  end

  def <=>(other)
    if name.nil?
      -1
    elsif other.name.nil?
      1
    else
      name.downcase <=> other.name.downcase
    end
  end
end
