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
# License along with {library}; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

class RuleFailure < ActiveRecord::Base

  belongs_to :rule
  belongs_to :snapshot

  def to_hash_json
    json = {}
    json['message'] = message
    json['line'] = line if line
    json['priority'] = Sonar::RulePriority.to_s(failure_level).upcase
    json['rule'] = {
      :key => rule.key,
      :name => rule.name,
      :category => rule.category.name}
    json['resource'] = {
      :key => snapshot.project.key,
      :name => snapshot.project.name,
      :scope => snapshot.project.scope,
      :qualifier => snapshot.project.qualifier,
      :language => snapshot.project.language}
    json
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0))
    xml.violation do
      xml.message(message)
      xml.line(line) if line
      xml.priority(Sonar::RulePriority.to_s(failure_level))
      xml.rule do
        xml.key(rule.key)
        xml.name(rule.name)
        xml.category(rule.category.name)
      end
      xml.resource do
        xml.key(snapshot.project.key)
        xml.name(snapshot.project.name)
        xml.scope(snapshot.project.scope)
        xml.qualifier(snapshot.project.qualifier)
        xml.language(snapshot.project.language)
      end
    end
  end

end
