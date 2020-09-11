/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 */

package com.netflix.spinnaker.clouddriver.kubernetes.security;

import com.netflix.spinnaker.cats.module.CatsModule;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.KubernetesV2Provider;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.KubernetesV2CachingAgent;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.KubernetesV2CachingAgentDispatcher;
import com.netflix.spinnaker.clouddriver.security.ProviderUtils;
import com.netflix.spinnaker.credentials.CredentialsLifecycleHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KubernetesCredentialsLifecycleHandler
    implements CredentialsLifecycleHandler<KubernetesNamedAccountCredentials> {
  protected final KubernetesV2Provider provider;
  protected final KubernetesV2CachingAgentDispatcher cachingAgentDispatcher;
  protected final CatsModule catsModule;

  @Override
  public void credentialsAdded(KubernetesNamedAccountCredentials credentials) {
    // Attempt to get namespaces to resolve any connectivity error without blocking /credentials
    List<String> namespaces = credentials.getCredentials().getDeclaredNamespaces();
    if (namespaces.isEmpty()) {
      log.warn(
          "New account {} did not return any namespace and could be unreachable or misconfigured",
          credentials.getName());
    }

    Collection<KubernetesV2CachingAgent> newlyAddedAgents =
        cachingAgentDispatcher.buildAllCachingAgents(credentials);

    log.info("Adding {} agents for new account {}", newlyAddedAgents.size(), credentials.getName());
    scheduleAgents(newlyAddedAgents);
  }

  @Override
  public void credentialsUpdated(KubernetesNamedAccountCredentials credentials) {
    // Attempt to get namespaces to resolve any connectivity error without blocking /credentials
    List<String> namespaces = credentials.getCredentials().getDeclaredNamespaces();
    if (namespaces.isEmpty()) {
      log.warn(
          "Modified account {} did not return any namespace and could be unreachable or misconfigured",
          credentials.getName());
    }

    Collection<KubernetesV2CachingAgent> updatedAgents =
        cachingAgentDispatcher.buildAllCachingAgents(credentials);

    log.info(
        "Scheduling {} agents for updated account {}", updatedAgents.size(), credentials.getName());
    // Remove existing agents belonging to changed accounts
    ProviderUtils.unscheduleAndDeregisterAgents(
        Collections.singleton(credentials.getName()), catsModule);
    scheduleAgents(updatedAgents);
  }

  protected void scheduleAgents(Collection<KubernetesV2CachingAgent> agents) {
    // If there is an agent scheduler, then this provider has been through the AgentController in
    // the past.
    // In that case, we need to do the scheduling here (because accounts have been added to a
    // running system).
    if (provider.getAgentScheduler() != null) {
      ProviderUtils.rescheduleAgents(provider, agents);
    }
    provider.addAgents(agents);
  }

  @Override
  public void credentialsDeleted(KubernetesNamedAccountCredentials credentials) {
    ProviderUtils.unscheduleAndDeregisterAgents(
        Collections.singleton(credentials.getName()), catsModule);
  }
}
