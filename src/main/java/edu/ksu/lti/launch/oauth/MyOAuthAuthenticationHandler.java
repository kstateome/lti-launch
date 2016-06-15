/**
 * Copyright 2014 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ksu.lti.launch.oauth;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth.provider.ConsumerAuthentication;
import org.springframework.security.oauth.provider.ConsumerCredentials;
import org.springframework.security.oauth.provider.OAuthAuthenticationHandler;
import org.springframework.security.oauth.provider.token.OAuthAccessProviderToken;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;

@Component
public class MyOAuthAuthenticationHandler implements OAuthAuthenticationHandler {

    private static final Logger LOG = Logger.getLogger(MyOAuthAuthenticationHandler.class);

    private static SimpleGrantedAuthority userGA = new SimpleGrantedAuthority("ROLE_USER");
    private static SimpleGrantedAuthority adminGA = new SimpleGrantedAuthority("ROLE_ADMIN");

    @PostConstruct
    public void init() {
        LOG.info("Oauth Authentication handler init");
    }

    @Override
    public Authentication createAuthentication(HttpServletRequest request, ConsumerAuthentication authentication, OAuthAccessProviderToken authToken) {
        Collection<GrantedAuthority> authorities = new HashSet<>(authentication.getAuthorities());
        // attempt to create a user Authority
        String username = request.getParameter("custom_canvas_user_login_id");
        if (StringUtils.isBlank(username)) {
            username = authentication.getName();
        }

        //honestly this is probably useless here. We need to do more permissions checking in the controllers anyway but it's part of the example OAuth setup so leaving for now
        if(StringUtils.isNotBlank(request.getParameter("roles")) && request.getParameter("roles").contains("Administrator")) {
            authorities.add(userGA);
            authorities.add(adminGA);
        } else {
            authorities.add(userGA);
        }

        Principal principal = new NamedOAuthPrincipal(username, authorities,
                authentication.getConsumerCredentials().getConsumerKey(),
                authentication.getConsumerCredentials().getSignature(),
                authentication.getConsumerCredentials().getSignatureMethod(),
                authentication.getConsumerCredentials().getSignatureBaseString(),
                authentication.getConsumerCredentials().getToken()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        LOG.info("createAuthentication generated auth principal (" + principal + "): req=" + request);
        return auth;
    }

    public static class NamedOAuthPrincipal extends ConsumerCredentials implements Principal {
        private static final long serialVersionUID = 1L;

        private String name;
        private Collection<GrantedAuthority> authorities;

        public NamedOAuthPrincipal(String name, Collection<GrantedAuthority> authorities, String consumerKey, String signature, String signatureMethod, String signatureBaseString, String token) {
            super(consumerKey, signature, signatureMethod, signatureBaseString, token);
            this.name = name;
            this.authorities = authorities;
        }

        @Override
        public String getName() {
            return name;
        }

        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String toString() {
            return "NamedOAuthPrincipal{" +
                    "name='" + name + '\'' +
                    ", key='" + getConsumerKey() + '\'' +
                    ", base='" + getSignatureBaseString() + '\'' +
                    "}";
        }
    }

}
