/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.password;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class PasswordProducer {

  private static final Logger LOG = LoggerFactory.getLogger(PasswordProducer.class);

  private static ConcurrentHashMap<String, BlockingQueue<char[]>> namePasswordsMap =
      new ConcurrentHashMap<>();

  private static ConcurrentHashMap<String, Boolean> nameResultMap = new ConcurrentHashMap<>();

  public static void registerPasswordConsumer(String name) {
    ParamUtil.requireNonBlank("name", name);
    BlockingQueue<char[]> queue = new LinkedBlockingQueue<>(1);
    nameResultMap.remove(name);
    namePasswordsMap.put(name, queue);
    final String str = "registered password consumer " + name;
    System.out.println(str);
    LOG.info(str);
  }

  public static void unregisterPasswordConsumer(String name) {
    ParamUtil.requireNonBlank("name", name);
    namePasswordsMap.remove(name);
    final String str = "unregistered password consumer " + name;
    System.out.println(str);
    LOG.info(str);
  }

  public static void setPasswordCorrect(String name, boolean correct) {
    ParamUtil.requireNonBlank("name", name);
    nameResultMap.put(name, correct);
    final String str = "set result of password consumer " + name + ": "
        + (correct ? "valid" : "invalid");
    System.out.println(str);
    LOG.info(str);
  }

  public static Boolean removePasswordCorrect(String name) {
    return nameResultMap.remove(name);
  }

  public static char[] takePassword(String name)
      throws InterruptedException, PasswordResolverException {
    ParamUtil.requireNonBlank("name", name);
    if (!namePasswordsMap.containsKey(name)) {
      throw new PasswordResolverException("password consumer '" + name + "' is not registered ");
    }
    char[] pwd = namePasswordsMap.get(name).take();
    final String str = "took password consumer " + name;
    System.out.println(str);
    LOG.info(str);
    return pwd;
  }

  public static void putPassword(String name, char[] password)
      throws InterruptedException, PasswordResolverException {
    ParamUtil.requireNonBlank("name", name);
    if (!namePasswordsMap.containsKey(name)) {
      throw new PasswordResolverException("password consumer '" + name + "' is not registered ");
    }

    nameResultMap.remove(name);
    namePasswordsMap.get(name).put(password);
    final String str = "provided password consumer " + name;
    System.out.println(str);
    LOG.info(str);
  }

  public static boolean needsPassword(String name) {
    ParamUtil.requireNonBlank("name", name);
    if (!namePasswordsMap.containsKey(name)) {
      return false;
    }
    return namePasswordsMap.get(name).isEmpty();
  }

  public static Set<String> getNames() {
    return Collections.unmodifiableSet(namePasswordsMap.keySet());
  }

}
