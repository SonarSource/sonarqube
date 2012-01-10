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
# License along with {library}; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

class RuleFailure < ActiveRecord::Base

  belongs_to :rule
  belongs_to :snapshot
  has_one :review, :primary_key => "permanent_id", :foreign_key => "rule_failure_permanent_id", :order => "created_at"
  after_save :update_permanent_id
  validates_presence_of :rule, :snapshot

  # first line of message
  def title
    @title||=
      begin
        if message.blank?
          rule.name
        else
          parts=Api::Utils.split_newlines(message)
          parts.size==0 ? rule.name : parts[0]
        end
      end
  end

  def plain_message
    @plain_message ||=
      begin
        Api::Utils.convert_string_to_unix_newlines(message)
      end
  end

  def html_message
    @html_message ||=
      begin
        message ? Api::Utils.split_newlines(ERB::Util.html_escape(message)).join('<br/>') : ''
      end
  end

  def severity
    Sonar::RulePriority.to_s(failure_level)
  end

  def resource
    snapshot.resource
  end

  def to_json(include_review=false, convert_markdown=false)
    json = {}
    json['id'] = id
    json['message'] = plain_message if plain_message
    json['line'] = line if line && line>=1
    json['priority'] = severity
    json['switchedOff']=true if switched_off?
    if created_at
      json['createdAt'] = Api::Utils.format_datetime(created_at)
    end
    json['rule'] = {
      :key => rule.key,
      :name => rule.name
    }
    json['resource'] = {
      :key => resource.key,
      :name => resource.name,
      :scope => resource.scope,
      :qualifier => resource.qualifier,
      :language => resource.language
    }
    json['review'] = review.to_json(convert_markdown) if include_review && review
    json
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0), include_review=false, convert_markdown=false)
    xml.violation do
      xml.id(id)
      xml.message(plain_message) if plain_message
      xml.line(line) if line && line>=1
      xml.priority(severity)
      xml.switchedOff(true) if switched_off?
      if created_at
        xml.createdAt(Api::Utils.format_datetime(created_at))
      end
      xml.rule do
        xml.key(rule.key)
        xml.name(rule.name)
      end
      xml.resource do
        xml.key(resource.key)
        xml.name(resource.name)
        xml.scope(resource.scope)
        xml.qualifier(resource.qualifier)
        xml.language(resource.language)
      end
      review.to_xml(xml, convert_markdown) if include_review && review
    end
  end

  def build_review(options={})
    if self.review.nil?
      self.review=Review.new(
        {
          :status => Review::STATUS_OPEN,
          :severity => severity,
          :resource_line => line,
          :resource => resource,
          :title => title,
          :rule => rule,
          :manual_violation => false,
          :manual_severity => false
        }.merge(options))
    end
  end

  def create_review!(options={})
    build_review(options)
    self.review.save!
  end

  private
  def update_permanent_id
    if self.permanent_id.nil? && self.id
      self.permanent_id = self.id
      save!
    end
  end
end
