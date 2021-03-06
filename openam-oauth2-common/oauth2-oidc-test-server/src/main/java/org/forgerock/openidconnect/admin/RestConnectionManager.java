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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect.admin;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.oauth2.admin.TokenResource;

import javax.servlet.ServletException;

/**
 * @since 12.0.0
 */
public class RestConnectionManager {

    public static ConnectionFactory getConnectionFactory() throws ServletException {
        try {
            final Router router = new Router();
            router.addRoute("/token/", InjectorHolder.getInstance(TokenResource.class));
            router.addRoute("/client/", InjectorHolder.getInstance(ClientResource.class));
            final ConnectionFactory factory = Resources.newInternalConnectionFactory(router);
            return factory;
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }
}
