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
 */

package com.netflix.spinnaker.accounts.external;

import com.netflix.spinnaker.accounts.Account;
import com.netflix.spinnaker.accounts.ReloadableAccountRepository;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("accounts.external-source.enabled")
@RequiredArgsConstructor
public class ExternalSourceAccountRepositories implements BeanDefinitionRegistryPostProcessor {
  private final ExternalSourceAccountRepositoryConfig config;
  private List<ReloadableAccountRepository<?>> repositories;

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
      throws BeansException {
    config.getRepositories().entrySet().stream()
        .forEach(
            e -> {
              BeanDefinition bd = this.getAccountSourceBeanDefinition(e.getKey(), e.getValue());
              if (bd != null) {
                registry.registerBeanDefinition(e.getKey() + "externalSourceRepository", bd);
              }
            });
  }

  protected BeanDefinition getAccountSourceBeanDefinition(
      String type, ExternalSourceAccountRepositoryConfig.AccountRepository repository) {
    // Find repo with type
    Optional<ReloadableAccountRepository<?>> repo =
        repositories.stream().filter(r -> r.getType().equals(type)).findFirst();

    if (!repo.isPresent()) {
      return null;
    }

    // Search for first hierarchy for the type of the AccountSource
    Class<? extends Account> propClass = getAccountPropertiesClass(repo.get());
    if (propClass == null) {
      return null;
    }

    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setBeanClass(RemoteAccountSource.class);
    bd.setConstructorArgumentValues(new ConstructorArgumentValues());
    bd.getConstructorArgumentValues().addGenericArgumentValue(repository.getEndpoint());
    bd.getConstructorArgumentValues().addGenericArgumentValue(propClass);
    return bd;
  }

  protected Class<? extends Account> getAccountPropertiesClass(
      ReloadableAccountRepository<?> repository) {
    for (Type type : repository.getClass().getGenericInterfaces()) {
      if (type.getClass().isAssignableFrom(Account.class)) {
        return (Class<? extends Account>) type.getClass();
      }
    }
    return null;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {}
}
