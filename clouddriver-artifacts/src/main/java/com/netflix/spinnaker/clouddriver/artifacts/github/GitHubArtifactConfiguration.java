/*
 * Copyright 2017 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.artifacts.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.accounts.AccountRepository;
import com.netflix.spinnaker.accounts.AccountRepositoryDescriptor;
import com.netflix.spinnaker.accounts.AccountSource;
import com.netflix.spinnaker.accounts.AccountSynchronizer;
import com.squareup.okhttp.OkHttpClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("artifacts.github.enabled")
@EnableConfigurationProperties(GitHubArtifactProviderProperties.class)
@RequiredArgsConstructor
@Slf4j
class GitHubArtifactConfiguration {
  private final GitHubArtifactProviderProperties gitHubArtifactProviderProperties;
  private final ObjectMapper objectMapper;

  @Bean
  @ConditionalOnMissingBean(
      value = GitHubArtifactCredentials.class,
      parameterizedContainer = AccountRepository.class)
  AccountRepository<GitHubArtifactCredentials> bitbucketRepository(
      OkHttpClient okHttpClient,
      AccountSynchronizer accountSynchronizer,
      Optional<AccountSource<GitHubArtifactAccount>> customAccountSource,
      @Value("${account.artifacts.github.refreshFrequencyMs:0}") long refreshFrequencyMs) {
    return AccountRepositoryDescriptor.<GitHubArtifactCredentials, GitHubArtifactAccount>builder()
        .type("BitBucket")
        .accountSynchronizer(accountSynchronizer)
        .customAccountSource(customAccountSource)
        .springAccountSource(gitHubArtifactProviderProperties::getAccounts)
        .refreshFrequencyMs(refreshFrequencyMs)
        .parser(
            a -> {
              try {
                return new GitHubArtifactCredentials(a, okHttpClient, objectMapper);
              } catch (Exception e) {
                log.warn("Failure instantiating GitHub artifact account {}: ", a, e);
                return null;
              }
            })
        .build()
        .createRepository();
  }

  //  @Bean
  //  List<? extends GitHubArtifactCredentials> gitHubArtifactCredentials(OkHttpClient okHttpClient)
  // {
  //    return gitHubArtifactProviderProperties.getAccounts().stream()
  //        .map(
  //            a -> {
  //              try {
  //                return new GitHubArtifactCredentials(a, okHttpClient, objectMapper);
  //              } catch (Exception e) {
  //                log.warn("Failure instantiating GitHub artifact account {}: ", a, e);
  //                return null;
  //              }
  //            })
  //        .filter(Objects::nonNull)
  //        .collect(Collectors.toList());
  //  }
}
