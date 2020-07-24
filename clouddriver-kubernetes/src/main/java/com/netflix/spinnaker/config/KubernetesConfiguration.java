/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.accounts.*;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.KubernetesV2Provider;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.clouddriver.kubernetes.health.KubernetesHealthIndicator;
import com.netflix.spinnaker.clouddriver.kubernetes.model.ManifestProvider;
import com.netflix.spinnaker.clouddriver.kubernetes.model.NoopManifestProvider;
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesNamedAccountCredentials;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty("kubernetes.enabled")
@ComponentScan({"com.netflix.spinnaker.clouddriver.kubernetes"})
public class KubernetesConfiguration {
  @Bean
  @RefreshScope
  @ConfigurationProperties("kubernetes")
  public KubernetesConfigurationProperties kubernetesConfigurationProperties() {
    return new KubernetesConfigurationProperties();
  }

  // If we have a reloadable repository, we schedule it to be reloaded
  @ConditionalOnBean(
      value = KubernetesNamedAccountCredentials.class,
      parameterizedContainer = ReloadableAccountRepository.class)
  public Reloader<KubernetesNamedAccountCredentials> reloadAccounts(
      ReloadableAccountRepository<KubernetesNamedAccountCredentials> accountRepository,
      @Value("${accounts.kubernetes.frequencyMs") long frequencyMs) {
    return new Reloader<>(accountRepository, frequencyMs);
  }

  // If externalSource is used, configure it
  @Bean
  @ConditionalOnProperty("accounts.kubernetes.externalSource.endpoint")
  public AccountSource<KubernetesConfigurationProperties.ManagedAccount> getExternalSource(
      String endpoint, ObjectMapper objectMapper) {
    return new RemoteAccountSource<>(endpoint, objectMapper);
  }

  // If no custom Kubernetes repository, we'll make one
  @Bean
  @ConditionalOnBean(
      value = KubernetesConfigurationProperties.ManagedAccount.class,
      parameterizedContainer = AccountSource.class)
  public AccountRepository<KubernetesNamedAccountCredentials>
      kubernetesAccountRepositoryCustomSource(
          AccountSource<KubernetesConfigurationProperties.ManagedAccount> accountSource,
          AccountParser<
                  KubernetesConfigurationProperties.ManagedAccount,
                  KubernetesNamedAccountCredentials>
              parser,
          AccountEventHandler<KubernetesNamedAccountCredentials> eventHandler) {
    return new MapBackedAccountRepository<>(accountSource, parser, eventHandler);
  }

  @Bean
  @ConditionalOnMissingBean(
      value = KubernetesNamedAccountCredentials.class,
      parameterizedContainer = AccountRepository.class)
  public AccountRepository<KubernetesNamedAccountCredentials> kubernetesAccountRepository(
      KubernetesConfigurationProperties configurationProperties,
      AccountParser<
              KubernetesConfigurationProperties.ManagedAccount, KubernetesNamedAccountCredentials>
          parser,
      AccountEventHandler<KubernetesNamedAccountCredentials> eventHandler) {
    return new MapBackedAccountRepository<>(
        configurationProperties.getAccounts(), parser, eventHandler);
  }

  @Bean
  public KubernetesHealthIndicator kubernetesHealthIndicator(
      Registry registry, AccountCredentialsProvider accountCredentialsProvider) {
    return new KubernetesHealthIndicator(registry, accountCredentialsProvider);
  }

  @Bean
  public KubernetesV2Provider kubernetesV2Provider() {
    return new KubernetesV2Provider();
  }

  //  @Bean
  //  public KubernetesV2ProviderSynchronizable kubernetesV2ProviderSynchronizable(
  //      KubernetesV2Provider kubernetesV2Provider,
  //      AccountCredentialsRepository accountCredentialsRepository,
  //      KubernetesV2CachingAgentDispatcher kubernetesV2CachingAgentDispatcher,
  //      KubernetesConfigurationProperties kubernetesConfigurationProperties,
  //      KubernetesV2Credentials.Factory credentialFactory,
  //      CatsModule catsModule) {
  //    return new KubernetesV2ProviderSynchronizable(
  //        kubernetesV2Provider,
  //        accountCredentialsRepository,
  //        kubernetesV2CachingAgentDispatcher,
  //        kubernetesConfigurationProperties,
  //        credentialFactory,
  //        catsModule);
  //  }

  @Bean
  @ConditionalOnMissingBean(ManifestProvider.class)
  public ManifestProvider noopManifestProvider() {
    return new NoopManifestProvider();
  }
}
