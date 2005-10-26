/*
 * Copyright (C) 2005 Jeremy C. Haile
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the
 *
 * Free Software Foundation, Inc.
 * 59 Temple Place, Suite 330
 * Boston, MA 02111-1307
 * USA
 *
 * Or, you may view it online at
 * http://www.opensource.org/licenses/lgpl-license.php
 */

package org.jsecurity.ri.authc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.Authenticator;
import org.jsecurity.authc.event.AuthenticationEvent;
import org.jsecurity.authz.AuthorizationContext;
import org.jsecurity.ri.authc.event.AuthenticationEventSender;
import org.jsecurity.ri.authc.event.FailedAuthenticationEvent;
import org.jsecurity.ri.authc.event.SuccessfulAuthenticationEvent;
import org.jsecurity.ri.authz.AuthorizationContextFactory;
import org.jsecurity.ri.authz.support.SimpleAuthorizationContextFactory;

/**
 * Superclass for {@link Authenticator} implementations that performs the common work
 * of wrapping a returned {@link AuthorizationContext} using an {@link AuthorizationContextFactory}
 * and binding the context using an {@link AuthorizationContextBinder}.  Subclasses should
 * implement the {@link #doAuthenticate(org.jsecurity.authc.AuthenticationToken)} method.
 *
 * @since 0.1
 * @author Jeremy Haile
 */
public abstract class AbstractAuthenticator implements Authenticator {

    /*--------------------------------------------
    |             C O N S T A N T S             |
    ============================================*/

    /*--------------------------------------------
    |    I N S T A N C E   V A R I A B L E S    |
    ============================================*/
    /**
     * Commons logger.
     */
    protected Log logger = LogFactory.getLog( getClass() );

    /**
     * The factory used to wrap authorization context after authentication.
     */
    private AuthorizationContextFactory authContextFactory = new SimpleAuthorizationContextFactory();

    /**
     * The binder used to bind the authorization context so that it is accessible on subsequent
     * requests.
     */
    private AuthorizationContextBinder authzCtxBinder = new ThreadLocalAuthorizationContextBinder();

    private AuthenticationEventSender authcEventSender = null;


    /*--------------------------------------------
    |         C O N S T R U C T O R S           |
    ============================================*/

    /*--------------------------------------------
    |  A C C E S S O R S / M O D I F I E R S    |
    ============================================*/
    protected AuthorizationContextFactory getAuthorizationContextFactory() {
        return authContextFactory;
    }


    public void setAuthorizationContextFactory(AuthorizationContextFactory authContextFactory) {
        this.authContextFactory = authContextFactory;
    }


    public AuthorizationContextBinder getAuthorizationContextBinder() {
        return authzCtxBinder;
    }


    public void setAuthorizationContextBinder(AuthorizationContextBinder authContextBinder) {
        this.authzCtxBinder = authContextBinder;
    }

    public AuthenticationEventSender getAuthenticationEventSender() {
        return authcEventSender;
    }

    public void setAuthenticationEventSender( AuthenticationEventSender authcEventSender ) {
        this.authcEventSender = authcEventSender;
    }

    private void sendFailedEvent( AuthenticationToken token, AuthenticationException cause ) {
        AuthenticationEventSender sender = getAuthenticationEventSender();
        if ( sender != null ) {
            AuthenticationEvent event =
                new FailedAuthenticationEvent( this, token.getPrincipal(), cause );
            sender.send( event );
        }
    }

    private void sendSuccessEvent( AuthenticationInfo info ) {
        AuthenticationEventSender sender = getAuthenticationEventSender();
        if ( sender != null ) {
            AuthenticationEvent event =
                new SuccessfulAuthenticationEvent( this, info.getPrincipal() );
            sender.send( event );
        }
    }

    /*--------------------------------------------
    |               M E T H O D S               |
    ============================================*/

    public final AuthorizationContext authenticate(AuthenticationToken authenticationToken) throws AuthenticationException {

        if (logger.isInfoEnabled()) {
            logger.info("Authentication request received for token [" + authenticationToken + "]");
        }

        AuthenticationInfo authInfo;
        try {
            authInfo = doAuthenticate( authenticationToken );
        } catch (AuthenticationException e) {
            // Catch exception for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication failed for token [" + authenticationToken + "]", e);
            }

            sendFailedEvent( authenticationToken, e );

            throw e;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Authentication successful.  Returned authentication info: [" + authInfo + "]");
        }

        sendSuccessEvent( authInfo );

        AuthorizationContextFactory factory = getAuthorizationContextFactory();

        AuthorizationContext authzCtx = factory.createAuthorizationContext( authInfo );

        // Bind the context to the application
        getAuthorizationContextBinder().bindAuthorizationContext( authzCtx );

        return authzCtx;
    }

    protected abstract AuthenticationInfo doAuthenticate( AuthenticationToken token ) throws AuthenticationException;
}