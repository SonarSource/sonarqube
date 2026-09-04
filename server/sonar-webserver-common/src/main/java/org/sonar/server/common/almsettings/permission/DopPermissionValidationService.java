/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.common.almsettings.permission;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.auth.github.GithubAppPermissions;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

/**
 * Validates whether a configured DevOps Platform instance grants the write permissions the SonarQube Remediation Agent
 * needs to clone a repository, push a branch and open a pull/merge request. It reuses the existing per-platform
 * validators — the same checks that run when minting an SCM token for the orchestrator — but returns a structured
 * {@link DopPermissionCheck} instead of throwing, so the outcome can be surfaced in the UI (SONAR-31626).
 *
 * <p>Only GitHub, GitLab and Azure DevOps are supported; Bitbucket is out of scope. {@link #check(AlmSettingDto)} and
 * {@link #checkAll(List)} always validate live. {@link #checkCached(AlmSettingDto)} and {@link #checkAllCached(List)}
 * (SONAR-31641) wrap those with an in-memory, per-node cache keyed by the {@code alm_setting}'s unique {@code key}, so
 * that projects sharing one DevOps Platform configuration share one cached verdict instead of each triggering a live
 * external call.
 */
@ServerSide
public class DopPermissionValidationService {

  private static final Logger LOG = LoggerFactory.getLogger(DopPermissionValidationService.class);

  // Short TTL: enough to collapse a burst of near-simultaneous requests for the same configuration (e.g. several
  // projects sharing one DevOps Platform config, loaded within the same page render) without keeping a stale verdict
  // around for long — there is no manual refresh action, so this TTL is the only way a corrected configuration
  // becomes visible again.
  private static final Duration CACHE_TTL = Duration.ofSeconds(60);
  private static final long MAX_CACHE_ENTRIES = 500;

  private static final String INSUFFICIENT_SCOPE_MARKER = "insufficient scope";

  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  private final AzureDevOpsValidator azureDevOpsValidator;
  private final System2 system2;
  // Not final: rebuilt by createForTesting() with a fake Ticker. See that method's javadoc for why this can't be a
  // second constructor overload instead.
  private Cache<String, TimestampedPermissionCheck> cache;

  public DopPermissionValidationService(GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator, AzureDevOpsValidator azureDevOpsValidator, System2 system2) {
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.gitlabGlobalSettingsValidator = gitlabGlobalSettingsValidator;
    this.azureDevOpsValidator = azureDevOpsValidator;
    this.system2 = system2;
    // Cache expiry uses a real monotonic ticker, not System2#now(): Guava's Ticker contract expects a monotonic
    // nanosecond source, and wall-clock time can jump (NTP correction, manual change), which would make entries expire
    // early or linger past the intended TTL. System2 is still used for the checkedAt value returned to API callers —
    // that's a display timestamp callers expect to be wall-clock, a different concern from cache bookkeeping.
    this.cache = buildCache(Ticker.systemTicker());
  }

  private static Cache<String, TimestampedPermissionCheck> buildCache(Ticker ticker) {
    return CacheBuilder.newBuilder()
      .expireAfterWrite(CACHE_TTL)
      .maximumSize(MAX_CACHE_ENTRIES)
      .ticker(ticker)
      .build();
  }

  /**
   * Package-private, test-only: builds an instance whose cache is driven by a fake {@link Ticker} instead of a real
   * monotonic clock, so TTL expiry can be exercised deterministically without sleeping past a real TTL.
   *
   * <p>This is a static factory rather than a second constructor overload on purpose: this class is constructor-injected
   * into {@code DefaultPermissionChecksController}, a Spring-managed bean. Spring uses a class's sole constructor
   * automatically, but with two or more constructors and no {@code @Autowired} to disambiguate, it falls back to
   * looking for a no-arg constructor and fails server startup — exactly what broke CI when this class briefly had a
   * second (package-private) constructor for this same purpose.
   */
  static DopPermissionValidationService createForTesting(GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator, AzureDevOpsValidator azureDevOpsValidator, System2 system2, Ticker cacheTicker) {
    DopPermissionValidationService service = new DopPermissionValidationService(githubGlobalSettingsValidator, gitlabGlobalSettingsValidator,
      azureDevOpsValidator, system2);
    service.cache = buildCache(cacheTicker);
    return service;
  }

  /**
   * Checks the given DevOps Platform configuration against the Remediation Agent's required write permissions.
   *
   * @throws IllegalArgumentException if the configuration's platform is not supported (Bitbucket).
   */
  public DopPermissionCheck check(AlmSettingDto almSetting) {
    return switch (almSetting.getAlm()) {
      case GITHUB -> checkGithub(almSetting);
      case GITLAB -> checkGitlab(almSetting);
      case AZURE_DEVOPS -> checkAzure(almSetting);
      case BITBUCKET, BITBUCKET_CLOUD ->
        throw new IllegalArgumentException("DevOps Platform '" + almSetting.getAlm() + "' is not supported by the Remediation Agent");
    };
  }

  /**
   * Checks several configurations in parallel and returns the results in the same order as the input. Each individual
   * check is time-bounded by the ALM client's connect/read timeouts; running them concurrently keeps the total close to
   * the slowest single platform. Uses a virtual thread per check rather than a fixed pool — these calls are blocking
   * I/O, not CPU-bound, and there are at most a handful of supported platforms per instance, so there's no pool sizing
   * to tune and no thread reuse to lose by not sharing an executor across calls. All settings must be supported
   * platforms (see {@link #check(AlmSettingDto)}).
   */
  public List<DopPermissionCheck> checkAll(List<AlmSettingDto> almSettings) {
    return mapInParallel(almSettings, this::check);
  }

  /**
   * Cached counterpart to {@link #check(AlmSettingDto)}. A cache hit performs no external call. Concurrent misses for
   * the same configuration are coalesced into a single live check (Guava {@code Cache.get(key, Callable)} semantics).
   */
  public TimestampedPermissionCheck checkCached(AlmSettingDto almSetting) {
    String key = almSetting.getKey();
    try {
      // Cache#get(key, Callable) declares ExecutionException for any checked exception the loader might throw; our
      // loader (check(AlmSettingDto)) never declares one, so this branch is unreachable in practice, only required by
      // the method signature. An unsupported platform (Bitbucket) throws IllegalArgumentException, which Guava
      // propagates unwrapped as UncheckedExecutionException rather than through this catch.
      TimestampedPermissionCheck result = cache.get(key, () -> new TimestampedPermissionCheck(check(almSetting), system2.now()));
      if (result.check().status() == PermissionCheckStatus.CHECK_FAILED) {
        // Don't let a transient failure (network blip, brief platform outage) stay pinned for the full TTL — evict it
        // immediately so the next request re-checks live instead of waiting for the entry to expire.
        cache.invalidate(key);
      }
      return result;
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to compute permission check for '" + almSetting.getKey() + "'", e.getCause());
    }
  }

  /**
   * Cached counterpart to {@link #checkAll(List)}. Results are returned in the same order as the input. Cache misses
   * (cold entries, or every entry right after startup) are validated in parallel just like {@link #checkAll(List)}, so
   * a cold cache doesn't serialize N external calls.
   */
  public List<TimestampedPermissionCheck> checkAllCached(List<AlmSettingDto> almSettings) {
    return mapInParallel(almSettings, this::checkCached);
  }

  private static <T> List<T> mapInParallel(List<AlmSettingDto> almSettings, Function<AlmSettingDto, T> mapper) {
    if (almSettings.size() <= 1) {
      return almSettings.stream().map(mapper).toList();
    }
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<T>> futures = almSettings.stream()
        .map(almSetting -> CompletableFuture.supplyAsync(() -> mapper.apply(almSetting), executor))
        .toList();
      return futures.stream().map(CompletableFuture::join).toList();
    }
  }

  private DopPermissionCheck checkGithub(AlmSettingDto almSetting) {
    try {
      List<String> missingPermissions = githubGlobalSettingsValidator.findMissingPermissions(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS);
      return missingPermissions.isEmpty() ? DopPermissionCheck.sufficient() : DopPermissionCheck.insufficient();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  private DopPermissionCheck checkGitlab(AlmSettingDto almSetting) {
    if (isUnsupportedGitlabToken(almSetting)) {
      return DopPermissionCheck.unsupportedTokenType();
    }
    try {
      return gitlabGlobalSettingsValidator.hasApiScope(almSetting) ? DopPermissionCheck.sufficient() : DopPermissionCheck.insufficient();
    } catch (GitlabServerException e) {
      if (e.getHttpStatus() == HTTP_FORBIDDEN && hasInsufficientScope(e.getMessage())) {
        return DopPermissionCheck.insufficient();
      }
      return DopPermissionCheck.checkFailed();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  /**
   * Runs independently of the scope-based check above: a GitLab bot token (Project/Group Access Token) can have
   * full read/write scope and still be unusable by the Remediation Agent, which needs to exchange the credential for
   * a short-lived user token (SONAR-31770). Any failure while determining bot status falls through to the
   * {@code hasApiScope()} check above, which already classifies invalid-credential/network failures.
   */
  private boolean isUnsupportedGitlabToken(AlmSettingDto almSetting) {
    try {
      return gitlabGlobalSettingsValidator.isBotToken(almSetting);
    } catch (Exception e) {
      LOG.debug("Could not determine whether the GitLab token for configuration '{}' belongs to a bot user; "
        + "falling back to the scope-based check", almSetting.getKey(), e);
      return false;
    }
  }

  private DopPermissionCheck checkAzure(AlmSettingDto almSetting) {
    try {
      azureDevOpsValidator.validate(almSetting);
      // Azure DevOps exposes no token-scope introspection, so a successful connectivity check cannot confirm write access.
      return DopPermissionCheck.unknown();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  private static boolean hasInsufficientScope(@Nullable String message) {
    return message != null && message.toLowerCase(Locale.ENGLISH).contains(INSUFFICIENT_SCOPE_MARKER);
  }
}
