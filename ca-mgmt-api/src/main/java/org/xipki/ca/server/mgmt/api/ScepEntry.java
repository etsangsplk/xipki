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

package org.xipki.ca.server.mgmt.api;

import java.util.Set;

import org.xipki.ca.api.NameId;
import org.xipki.common.InvalidConfException;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.CompareUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class ScepEntry {

  private final String name;

  private final NameId caIdent;

  private final boolean active;

  private final Set<String> certprofiles;

  private final String control;

  private String responderName;

  public ScepEntry(String name, NameId caIdent, boolean active, String responderName,
      Set<String> certprofiles, String control) throws InvalidConfException {
    this.name = ParamUtil.requireNonBlankLower("name", name);
    this.caIdent = ParamUtil.requireNonNull("caIdent", caIdent);
    this.active = active;
    this.responderName = ParamUtil.requireNonBlank("responderName", responderName);
    this.certprofiles = CollectionUtil.unmodifiableSet(
        CollectionUtil.toLowerCaseSet(certprofiles));
    this.control = control;
  }

  public String getName() {
    return name;
  }

  public boolean isActive() {
    return active;
  }

  public Set<String> getCertprofiles() {
    return certprofiles;
  }

  public String getControl() {
    return control;
  }

  public void setResponderName(String responderName) {
    this.responderName = responderName;
  }

  public String getResponderName() {
    return responderName;
  }

  public NameId getCaIdent() {
    return caIdent;
  }

  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean ignoreSensitiveInfo) {
    return StringUtil.concatObjects("ca: ", caIdent, "\nactive: ", active,
        "\nresponderName: ", responderName, "\ncontrol: ", control);
  } // method toString

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ScepEntry)) {
      return false;
    }

    return equals((ScepEntry) obj, false);
  }

  public boolean equals(ScepEntry obj, boolean ignoreId) {
    return (obj != null)
        && caIdent.equals(obj.caIdent, ignoreId)
        && (active == obj.active)
        && responderName.equals(obj.responderName)
        && CompareUtil.equalsObject(control, obj.control);
  }

  @Override
  public int hashCode() {
    return caIdent.hashCode();
  }

}
