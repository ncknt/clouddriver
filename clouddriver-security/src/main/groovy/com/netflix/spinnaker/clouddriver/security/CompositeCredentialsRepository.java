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

package com.netflix.spinnaker.clouddriver.security;

import com.netflix.spinnaker.accounts.CredentialsRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is here while some of the accounts are not migrated to CredentialsRepository. When
 * migration is complete we can use CompositeCredentialsRepository directly
 */
public class CompositeCredentialsRepository
    extends com.netflix.spinnaker.accounts.CompositeCredentialsRepository<AccountCredentials> {
  private AccountCredentialsProvider accountCredentialsProvider;

  public CompositeCredentialsRepository(
      List<CredentialsRepository<? extends AccountCredentials>> credentialsRepositories,
      AccountCredentialsProvider accountCredentialsProvider) {
    super(credentialsRepositories);
    this.accountCredentialsProvider = accountCredentialsProvider;
  }

  @Override
  public AccountCredentials getCredentials(String accountName, String type) {
    AccountCredentials credentials = super.getCredentials(accountName, type);
    if (credentials != null) {
      return credentials;
    }
    return accountCredentialsProvider.getCredentials(accountName);
  }

  @Override
  public List<AccountCredentials> getAllCredentials() {
    List<AccountCredentials> all = new ArrayList<>(accountCredentialsProvider.getAll());
    all.addAll(super.getAllCredentials());
    return all;
  }
}
