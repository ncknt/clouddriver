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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.accounts.Account;
import com.netflix.spinnaker.accounts.AccountSource;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RemoteAccountSource<T extends Account> implements AccountSource<T> {
  protected final String endpoint;
  protected final Class<T> propertiesClass;
  protected final ObjectMapper objectMapper = new ObjectMapper();

  protected List<T> doCall() throws IOException {
    objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(endpoint).build();
    Response response = client.newCall(request).execute();

    JsonParser parser = objectMapper.getFactory().createParser(response.body().byteStream());

    if (parser.nextToken() != JsonToken.START_ARRAY) {
      throw new IllegalArgumentException("not good");
    }
    List<T> result = new ArrayList<>();
    while (parser.nextToken() == JsonToken.START_OBJECT) {
      result.add(objectMapper.readValue(parser, propertiesClass));
    }
    return result;
  }

  @Override
  public List<T> getAccounts() {
    try {
      return doCall();
    } catch (IOException e) {
      // TODO backoff, retry
      throw new RuntimeException(e);
    }
  }
}
