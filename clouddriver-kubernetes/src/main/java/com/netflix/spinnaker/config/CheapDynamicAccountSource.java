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
 *
 */

package com.netflix.spinnaker.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@Component
@ConditionalOnProperty("credentials.files.kubernetes")
public class CheapDynamicAccountSource
    implements CredentialsDefinitionSource<KubernetesConfigurationProperties.ManagedAccount> {
  private final ObjectMapper objectMapper;
  private final String fileName;
  private final Yaml yaml = new Yaml(new SafeConstructor());

  public CheapDynamicAccountSource(
      ObjectMapper objectMapper, @Value("${credentials.files.kubernetes}") String fileName) {
    this.objectMapper = objectMapper;
    this.fileName = fileName;
  }

  @Override
  public List<KubernetesConfigurationProperties.ManagedAccount> getCredentialsDefinitions() {
    try (Reader reader = Files.newBufferedReader(Paths.get(fileName), Charset.defaultCharset())) {
      return objectMapper.convertValue(
          yaml.load(reader),
          new TypeReference<List<KubernetesConfigurationProperties.ManagedAccount>>() {});
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read credential information", e);
    }
  }
}
