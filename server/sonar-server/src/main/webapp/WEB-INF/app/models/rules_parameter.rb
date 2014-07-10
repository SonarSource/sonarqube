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
# License along with {library}; if not, write to the Free Software
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
class RulesParameter < ActiveRecord::Base
  include RulesConfigurationHelper

  PARAM_MAX_NUMBER = 4

  validates_presence_of :name, :param_type
  belongs_to :rule

  def to_hash_json(active_rule)
    json = {'name' => name}
    json['description']=description if description
    if active_rule
      active_parameter = active_rule.active_param_by_param_id(id)
      json['value'] = active_parameter.value if active_parameter
    end
    json
  end

  def to_xml(active_rule, xml)
    xml.param do
      xml.name(name)
      xml.description { xml.cdata!(description) } if description
      if active_rule
        active_parameter = active_rule.active_param_by_param_id(id)
        xml.value(active_parameter.value) if active_parameter
      end
    end
  end

  def <=>(other)
    name <=> other.name
  end
end
