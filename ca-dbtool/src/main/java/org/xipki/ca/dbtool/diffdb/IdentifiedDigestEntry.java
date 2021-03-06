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

package org.xipki.ca.dbtool.diffdb;

import org.xipki.common.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

class IdentifiedDigestEntry {

  private final DigestEntry content;

  private Integer caId;

  private final long id;

  public IdentifiedDigestEntry(DigestEntry content, long id) {
    this.content = ParamUtil.requireNonNull("content", content);
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public DigestEntry getContent() {
    return content;
  }

  public void setCaId(Integer caId) {
    this.caId = caId;
  }

  public Integer getCaId() {
    return caId;
  }

}
