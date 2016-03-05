/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2013 - 2016 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License (version 3
 * or later at your option) as published by the Free Software Foundation
 * with the addition of the following permission added to Section 15 as
 * permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.commons.security.impl.p11.iaik;

import java.security.SecureRandom;

import org.xipki.commons.common.util.ParamUtil;

import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class IaikP11Util {

    private IaikP11Util() {
    }

    public static byte[] generateKeyId(
            final Session session)
    throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] keyId = null;
        do {
            keyId = new byte[8];
            random.nextBytes(keyId);
        } while (idExists(session, keyId));

        return keyId;
    }

    public static boolean idExists(
            final Session session, final byte[] keyId)
    throws Exception {
        ParamUtil.requireNonNull("session", session);
        ParamUtil.requireNonNull("keyId", keyId);

        Key k = new Key();
        k.getId().setByteArrayValue(keyId);

        session.findObjectsInit(k);
        Object[] objects = session.findObjects(1);
        session.findObjectsFinal();
        if (objects.length > 0) {
            return true;
        }

        X509PublicKeyCertificate c = new X509PublicKeyCertificate();
        c.getId().setByteArrayValue(keyId);

        session.findObjectsInit(c);
        objects = session.findObjects(1);
        session.findObjectsFinal();

        return objects.length > 0;
    }

    public static boolean labelExists(
            final Session session,
            final String keyLabel)
    throws Exception {
        ParamUtil.requireNonNull("session", session);
        ParamUtil.requireNonBlank("keyLabel", keyLabel);
        Key k = new Key();
        k.getLabel().setCharArrayValue(keyLabel.toCharArray());

        session.findObjectsInit(k);
        Object[] objects = session.findObjects(1);
        session.findObjectsFinal();
        if (objects.length > 0) {
            return true;
        }

        X509PublicKeyCertificate c = new X509PublicKeyCertificate();
        c.getLabel().setCharArrayValue(keyLabel.toCharArray());

        session.findObjectsInit(c);
        objects = session.findObjects(1);
        session.findObjectsFinal();

        return objects.length > 0;
    }

    static String eraseSensitiveInfo(
            final String data) {
        ParamUtil.requireNonNull("data", data);
        int index = data.indexOf("password");
        if (index == -1) {
            return data;
        }

        if (index > 1 && data.charAt(index - 1) == '%') {
            return data.substring(0, index - 1);
        } else {
            return data.substring(0, index);
        }
    }

}
