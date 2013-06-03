#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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

class Issue
  def self.to_hash(issue)
    hash = {
      :key => issue.key,
      :component => issue.componentKey,
      :project => issue.projectKey,
      :rule => issue.ruleKey.toString(),
      :status => issue.status
    }
    hash[:resolution] = issue.resolution if issue.resolution
    hash[:severity] = issue.severity if issue.severity
    hash[:message] = issue.message if issue.message
    hash[:line] = issue.line.to_i if issue.line
    hash[:effortToFix] = issue.effortToFix.to_f if issue.effortToFix
    hash[:reporter] = issue.reporter if issue.reporter
    hash[:assignee] = issue.assignee if issue.assignee
    hash[:author] = issue.authorLogin if issue.authorLogin
    hash[:actionPlan] = issue.actionPlanKey if issue.actionPlanKey
    hash[:creationDate] = Api::Utils.format_datetime(issue.creationDate) if issue.creationDate
    hash[:updateDate] = Api::Utils.format_datetime(issue.updateDate) if issue.updateDate
    hash[:closeDate] = Api::Utils.format_datetime(issue.closeDate) if issue.closeDate
    hash[:attr] = issue.attributes.to_hash unless issue.attributes.isEmpty()
    if issue.comments.size>0
      hash[:comments] = issue.comments.map { |c| comment_to_hash(c) }
    end
    hash
  end

  def self.comment_to_hash(comment)
    {
      :key => comment.key(),
      :login => comment.userLogin(),
      :htmlText => Internal.text.markdownToHtml(comment.markdownText()),
      :createdAt => Api::Utils.format_datetime(comment.createdAt())
    }
  end

end