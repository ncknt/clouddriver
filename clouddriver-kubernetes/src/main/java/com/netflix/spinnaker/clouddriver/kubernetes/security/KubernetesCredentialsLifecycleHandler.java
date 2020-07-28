/*
 * Copyright 2020 Netflix, Inc.
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
 */

package com.netflix.spinnaker.clouddriver.kubernetes.security;

import com.netflix.spinnaker.accounts.CredentialsLifecycleHandler;
import com.netflix.spinnaker.cats.agent.Agent;
import com.netflix.spinnaker.cats.module.CatsModule;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.KubernetesV2Provider;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.KubernetesV2CachingAgentDispatcher;
import com.netflix.spinnaker.clouddriver.security.ProviderUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
  public void accountAdded(KubernetesNamedAccountCredentials account) {
    // Attempt to get namespaces to resolve any connectivity error without blocking /credentials
    List<String> namespaces = account.getCredentials().getDeclaredNamespaces();
    if (namespaces.isEmpty()) {
      log.warn(
          "New or modified account {} did not return any namespace and could be unreachable or misconfigured",
          account.getName());
    }

    List<Agent> newlyAddedAgents =
        (List<Agent>)
            cachingAgentDispatcher.buildAllCachingAgents(account).stream()
                .map(c -> (Agent) c)
                .collect(Collectors.toList());

    log.info("Adding {} agents for account {}", newlyAddedAgents.size(), account.getName());
    provider.stageAgents(newlyAddedAgents);
  }

  @Override
  public void accountUpdated(KubernetesNamedAccountCredentials account) {
    // Attempt to get namespaces to resolve any connectivity error without blocking /credentials
    List<String> namespaces = account.getCredentials().getDeclaredNamespaces();
    if (namespaces.isEmpty()) {
      log.warn(
          "New or modified account {} did not return any namespace and could be unreachable or misconfigured",
          account.getName());
    }
  }

  @Override
  public void accountDeleted(KubernetesNamedAccountCredentials account) {
    ProviderUtils.unscheduleAndDeregisterAgents(
        Collections.singleton(account.getName()), catsModule);
  }
}
