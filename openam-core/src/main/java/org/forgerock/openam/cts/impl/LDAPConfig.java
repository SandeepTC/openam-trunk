
/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl;

import com.iplanet.am.util.SystemProperties;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.utils.ModifiedProperty;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.annotations.VisibleForTesting;

import javax.inject.Inject;

/**
 * Responsible for storing the configuration required by the LDAP implementation of the
 * Core Token Service backend.
 *
 * Note: This configuration data is partitioned into the impl package as it represents the
 * boundary of LDAP concepts within the Core Token Service code.
 */
public class LDAPConfig {

    private final DN defaultCTSRootSuffix;

    private ModifiedProperty<String> tokenStoreRootSuffix = new ModifiedProperty<String>();

    @Inject
    public LDAPConfig(String rootSuffix) {
        defaultCTSRootSuffix = DN.valueOf(rootSuffix)
                .child("ou=tokens")
                .child("ou=openam-session")
                .child("ou=famrecords");
        update();
    }

    /**
     * The Core Token Service Root Token Suffix.
     *
     * @return A fully qualified DN (distinguished name) for the Root Suffix where
     * Tokens will be stored. Maybe null.
     */
    public DN getTokenStoreRootSuffix() {
        String value = tokenStoreRootSuffix.get();
        if (StringUtils.isEmpty(value)) {
            return defaultCTSRootSuffix;
        }
        return DN.valueOf(value);
    }

    /**
     * @return True if the configuration has changed.
     */
    public boolean hasChanged() {
        return tokenStoreRootSuffix.hasChanged();
    }

    /**
     * {@inheritDoc}
     *
     * Will update its configuration from the System Properties.
     */
    public void update() {
        tokenStoreRootSuffix.set(SystemProperties.get(CoreTokenConstants.CTS_ROOT_SUFFIX));
    }

    /**
     * @return the value of the default CTS root suffix
     */
    @VisibleForTesting
    DN getDefaultCTSRootSuffix() {
        return defaultCTSRootSuffix;
    }
}
