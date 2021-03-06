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

import java.math.BigInteger;
import java.util.Date;

import org.xipki.common.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.1.0
 */

public class CertListInfo {
  private final BigInteger serialNumber;

  private final Date notBefore;

  private final Date notAfter;

  private final String subject;

  public CertListInfo(BigInteger serialNumber, String subject, Date notBefore, Date notAfter) {
    this.serialNumber = ParamUtil.requireNonNull("serialNumber", serialNumber);
    this.notBefore = ParamUtil.requireNonNull("notBefore", notBefore);
    this.notAfter = ParamUtil.requireNonNull("notAfter", notAfter);
    this.subject = ParamUtil.requireNonNull("subject", subject);
  }

  public BigInteger getSerialNumber() {
    return serialNumber;
  }

  public Date getNotBefore() {
    return notBefore;
  }

  public Date getNotAfter() {
    return notAfter;
  }

  public String getSubject() {
    return subject;
  }

}
