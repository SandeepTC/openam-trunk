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

package org.forgerock.oauth2.core.exceptions;

/**
 * Thrown when client authentication fails.
 *
 * @since 12.0.0
 */
public class ClientAuthenticationFailedException extends OAuth2Exception {

    private final String headerName;
    private final String headerValue;

    /**
     * Constructs a new ClientAuthenticationFailedException with the specified message, header name and value.
     *
     * @param message The reason for the exception.
     * @param headerName The name of the response header.
     * @param headerValue The valud of the response header.
     */
    public ClientAuthenticationFailedException(final String message, final String headerName, final String headerValue) {
        super(401, "invalid_client", message);
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    /**
     * Gets the name of the response header.
     *
     * @return The response header name.
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * Gets the value of the response header.
     *
     * @return The response header value.
     */
    public String getHeaderValue() {
        return headerValue;
    }
}
