/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Function;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QualityProfile;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.transformValues;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Feed analysis metadata holder with metadata from the analysis report.
 */
public class LoadReportAnalysisMetadataHolderStep implements ComputationStep {

  private static final ToComputeQProfile TO_COMPUTE_QPROFILE = new ToComputeQProfile();

  private static final class ToComputeQProfile implements Function<QProfile, QualityProfile> {
    @Override
    public QualityProfile apply(QProfile input) {
      return new QualityProfile(input.getKey(), input.getName(), input.getLanguage(), new Date(input.getRulesUpdatedAt()));
    }
  }

  private final CeTask ceTask;
  private final BatchReportReader reportReader;
  private final MutableAnalysisMetadataHolder mutableAnalysisMetadataHolder;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final DbClient dbClient;

  public LoadReportAnalysisMetadataHolderStep(CeTask ceTask, BatchReportReader reportReader, MutableAnalysisMetadataHolder mutableAnalysisMetadataHolder,
    DefaultOrganizationProvider defaultOrganizationProvider, DbClient dbClient) {
    this.ceTask = ceTask;
    this.reportReader = reportReader;
    this.mutableAnalysisMetadataHolder = mutableAnalysisMetadataHolder;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.dbClient = dbClient;
  }

  @Override
  public void execute() {
    ScannerReport.Metadata reportMetadata = reportReader.readMetadata();
    mutableAnalysisMetadataHolder.setAnalysisDate(reportMetadata.getAnalysisDate());

    checkProjectKeyConsistency(reportMetadata);
    Organization organization = toOrganization(ceTask.getOrganizationUuid());
    checkOrganizationKeyConsistency(reportMetadata, organization);

    mutableAnalysisMetadataHolder.setRootComponentRef(reportMetadata.getRootComponentRef());
    mutableAnalysisMetadataHolder.setBranch(isNotEmpty(reportMetadata.getBranch()) ? reportMetadata.getBranch() : null);
    mutableAnalysisMetadataHolder.setCrossProjectDuplicationEnabled(reportMetadata.getCrossProjectDuplicationActivated());
    mutableAnalysisMetadataHolder.setQProfilesByLanguage(transformValues(reportMetadata.getQprofilesPerLanguage(), TO_COMPUTE_QPROFILE));
    mutableAnalysisMetadataHolder.setOrganization(organization);
  }

  private void checkProjectKeyConsistency(ScannerReport.Metadata reportMetadata) {
    String reportProjectKey = projectKeyFromReport(reportMetadata);
    String componentKey = ceTask.getComponentKey();
    if (componentKey == null) {
      throw MessageException.of(format(
        "Compute Engine task component key is null. Project with UUID %s must have been deleted since report was uploaded. Can not proceed.",
        ceTask.getComponentUuid()));
    }
    if (!componentKey.equals(reportProjectKey)) {
      throw MessageException.of(format(
        "ProjectKey in report (%s) is not consistent with projectKey under which the report as been submitted (%s)",
        reportProjectKey,
        componentKey));
    }
  }

  private void checkOrganizationKeyConsistency(ScannerReport.Metadata reportMetadata, Organization organization) {
    String organizationKey = reportMetadata.getOrganizationKey();
    String resolveReportOrganizationKey = resolveReportOrganizationKey(organizationKey);
    if (!resolveReportOrganizationKey.equals(organization.getKey())) {
      if (reportBelongsToDefaultOrganization(organizationKey)) {
        throw MessageException.of(format(
          "Report does not specify an OrganizationKey but it has been submitted to another organization (%s) than the default one (%s)",
          organization.getKey(),
          defaultOrganizationProvider.get().getKey()));
      } else {
        throw MessageException.of(format(
          "OrganizationKey in report (%s) is not consistent with organizationKey under which the report as been submitted (%s)",
          resolveReportOrganizationKey,
          organization.getKey()));
      }
    }
  }

  private String resolveReportOrganizationKey(@Nullable String organizationKey) {
    if (reportBelongsToDefaultOrganization(organizationKey)) {
      return defaultOrganizationProvider.get().getKey();
    }
    return organizationKey;
  }

  private static boolean reportBelongsToDefaultOrganization(@Nullable String organizationKey) {
    return organizationKey == null || organizationKey.isEmpty();
  }

  private Organization toOrganization(String organizationUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid);
      checkState(organizationDto.isPresent(), "Organization with uuid '{}' can't be found", organizationUuid);
      return Organization.from(organizationDto.get());
    }
  }

  private static String projectKeyFromReport(ScannerReport.Metadata reportMetadata) {
    if (isNotEmpty(reportMetadata.getBranch())) {
      return reportMetadata.getProjectKey() + ":" + reportMetadata.getBranch();
    }
    return reportMetadata.getProjectKey();
  }

  @Override
  public String getDescription() {
    return "Load analysis metadata";
  }
}
