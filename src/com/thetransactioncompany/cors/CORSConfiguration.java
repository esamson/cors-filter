package com.thetransactioncompany.cors;


import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.thetransactioncompany.util.PropertyParseException;
import com.thetransactioncompany.util.PropertyRetriever;


/**
 * The CORS filter configuration (typically originating from the web application
 * descriptor file {@code web.xml}). The fields become immutable (final) after 
 * they are initialised.
 *
 * @author Vladimir Dzhuvinov
 * @author Luis Sala
 * @author Jared Ottley
 * @version $version$ (2012-10-19)
 */
public class CORSConfiguration {
	
	
	/**
	 * If {@code true} generic HTTP requests must be allowed to pass through
	 * the filter, else only valid and accepted CORS requests must be 
	 * allowed (strict CORS filtering).
	 *
	 * <p>Property key: cors.allowGenericHttpRequests
	 */
	public final boolean allowGenericHttpRequests;


	/**
	 * If {@code true} the CORS filter must allow requests from any origin,
	 * else the origin whitelist {@link #allowedOrigins} must be consulted.
	 *
	 * <p>Property key: cors.allowOrigin (set to {@code *})
	 */
	public final boolean allowAnyOrigin;
	
	
	/**
	 * Whitelisted origins that the CORS filter must allow. Requests from 
	 * origins not included here must be refused with a HTTP 403 "Forbidden"
	 * response. This property is overriden by {@link #allowAnyOrigin}.
	 *
	 * <p>Note: The set is of type String instead of Origin to bypass
	 * parsing of the request origins before matching, see 
	 * http://lists.w3.org/Archives/Public/public-webapps/2010JulSep/1046.html
	 *
	 * <p>Property key: cors.allowOrigin
	 */
	public final Set<ValidatedOrigin> allowedOrigins;
	
	
	/**
	 * If {@code true} the CORS filter must allow requests from any origin
	 * which is a subdomain origin of the {@link #allowedOrigins}.
	 *
	 * <p>Example:
	 *
	 * <p>Explicitly allowed origin: {@code http://example.com}
	 *
	 * <p>Matches the original origin as well as any subdomain, e.g. 
	 * {@code http://foo.example.com}, {@code http://bar.example.com}, 
	 * etc...
	 *
	 * <p>Property key: cors.allowSubdomains
	 */
	public final boolean allowSubdomains;
	

	/**
	 * Helper method to check whether requests from the specified origin 
	 * must be allowed. This is done by looking up {@link #allowAnyOrigin} 
	 * and {@link #allowedOrigins} as well as the {@link #allowSubdomains}
	 * setting.
	 *
	 * @param origin The origin as reported by the web client (browser), 
	 *               {@code null} if unknown.
	 *
	 * @return {@code true} if the origin is allowed, else {@code false}.
	 */
	public final boolean isAllowedOrigin(final Origin origin) {

        	if (allowAnyOrigin)
                	return true;
		
		if (origin == null)
			return false;
		
		if (allowedOrigins.contains(origin))
			return true;
			
		else if (allowSubdomains)
			return isAllowedSubdomainOrigin(origin);
		
		return false;
	}
	
	/**
	 * Helper method to check whether the specified origin is a subdomain 
	 * origin of the {@link #allowedOrigins}. This is done by looking up the
	 * origin's scheme, hostname and port and matching them with each of the 
	 * {@link #allowedOrigins}.
	 *
	 * <p>Example: 
	 *
	 * <p>{@code Origin: https://foo.example.com } matches 
	 * {@code cors.allowedOrigin = https://example.com } whereas 
	 * {@code cors.allowedOrigin = http://example.com } would not match.
	 *
	 * @param origin The origin as reported by the web client (browser), 
	 *               {@code null} if unknown.
	 *
	 * @return {@code true} if the origin is an allowed subdomain origin, 
	 *         else {@code false}.
	 */
	public final boolean isAllowedSubdomainOrigin(final Origin origin) {
		
		try {
			ValidatedOrigin validatedOrigin = origin.validate();
			
			String scheme = validatedOrigin.getScheme();
			String suffix = validatedOrigin.getSuffix();
			
			for (ValidatedOrigin allowedOrigin: allowedOrigins) {
				
				if (suffix.endsWith("." + allowedOrigin.getSuffix()) && 
				    scheme.equalsIgnoreCase(allowedOrigin.getScheme()))

					return true;
			}
			
		} catch (OriginException e) {
    			
			return false;
		}
		
		return false;
	}
	
	
	/**
	 * The supported HTTP methods. Requests for methods not included here 
	 * must be refused by the CORS filter with a HTTP 405 "Method not 
	 * allowed" response.
	 *
	 * <p>Property key: cors.supportedMethods
	 */
	public final Set<HTTPMethod> supportedMethods;
	
	
	/**
	 * Helper method to check whether the specified HTTP method is 
	 * supported. This is done by looking up {@link #supportedMethods}.
	 *
	 * @param method The HTTP method.
	 *
	 * @return {@code true} if the method is supported, else {@code false}.
	 */
	public final boolean isSupportedMethod(final HTTPMethod method) {
	
		if (supportedMethods.contains(method))
			return true;
		else
			return false;
	}
	
	
	/**
	 * The names of the supported author request headers.
	 * 
	 * <p>Property key: cors.supportedHeaders
	 */
	public final Set<HeaderFieldName> supportedHeaders;
	
	
	/**
	 * Helper method to check whether the specified (non-simple) author 
	 * request header is supported. This is done by looking up 
	 * {@link #supportedHeaders}.
	 *
	 * @param header The header field name.
	 *
	 * @return {@code true} if the header is supported, else {@code false}.
	 */
	public final boolean isSupportedHeader(final HeaderFieldName header) {

        	if (supportedHeaders.contains(header))
			return true;
		else
			return false;
	}
	
	
	/**
	 * The non-simple response headers that the web browser should expose to 
	 * the author of the CORS request.
	 *
	 * <p>Property key: cors.exposedHeaders
	 */
	public final Set<HeaderFieldName> exposedHeaders;
	
	
	/**
	 * Indicates whether user credentials, such as cookies, HTTP 
	 * authentication or client-side certificates, are supported.
	 *
	 * <p>Property key: cors.supportsCredentials
	 */
	public final boolean supportsCredentials;
	
	
	/**
	 * Indicates how long the results of a preflight request can be cached
	 * by the web client, in seconds. If {@code -1} unspecified.
	 *
	 * <p>Property key: cors.maxAge
	 */
	public final int maxAge;
	
	
	/**
	 * Parses a string containing words separated by space and/or comma.
	 *
	 * @param s The string to parse. Must not be {@code null}.
	 *
	 * @return An array of the parsed words, empty if none were found.
	 */
	protected static String[] parseWords(final String s) {
		
		String s1 = s.trim();
		
		if (s1.isEmpty())
			return new String[]{};
		else
			return s1.split("\\s*,\\s*|\\s+");
	}
	
	
	/**
	 * Creates a new CORS configuration from the specified properties.
	 *
	 * <p>The following properties are recognised (if missing they default
	 * to the specified values):
	 *
	 * <ul>
	 *     <li>cors.allowGenericHttpRequests {true|false} defaults to 
	 *         {@code true}.
	 *     <li>cors.allowOrigin {"*"|origin-list} defaults to {@code *}.
	 *     <li>cors.allowSubdomains {true|false} defaults to {@code false}.
	 *     <li>cors.supportedMethods {method-list} defaults to {@code "GET, 
	 *         POST, HEAD, OPTIONS"}.
	 *     <li>cors.supportedHeaders {header-list} defaults to empty list.
	 *     <li>cors.exposedHeaders {header-list} defaults to empty list.
	 *     <li>cors.supportsCredentials {true|false} defaults to 
	 *         {@code true}.
	 *     <li>cors.maxAge {int} defaults to {@code -1} (unspecified).
	 * </ul>
	 *
	 * @param props The properties. Must not be {@code null}.
	 *
	 * @throws CORSConfigurationException On a invalid property.
	 */
	public CORSConfiguration(final Properties props)
		throws CORSConfigurationException {
	
		try {
			PropertyRetriever pr = new PropertyRetriever(props);

			// Parse the allow generic HTTP requests option
			
			allowGenericHttpRequests = pr.getOptBoolean("cors.allowGenericHttpRequests", true);
			
			// Parse the allowed origins list
			
			String originSpec = pr.getOptString("cors.allowOrigin", "*").trim();
			
			allowedOrigins = new HashSet<ValidatedOrigin>();

			if (originSpec.equals("*")) {

				allowAnyOrigin = true;
			}
			else {
				allowAnyOrigin = false;

				String[] urls = parseWords(originSpec);

				for (String url: urls) {

					try {
						allowedOrigins.add(new Origin(url).validate());

                                	} catch (OriginException e) {
					
                                        	throw new PropertyParseException("Bad origin URL in property cors.allowOrigin: " + url);
                                	}
				}
			}
			
			// Parse the allow origin suffix matching option
			allowSubdomains = pr.getOptBoolean("cors.allowSubdomains", false);
			

			// Parse the supported methods list

			String methodSpec = pr.getOptString("cors.supportedMethods", "GET, POST, HEAD, OPTIONS").trim().toUpperCase();

			String[] methodNames = parseWords(methodSpec);

			supportedMethods = new HashSet<HTTPMethod>();

			for (String methodName: methodNames) {

				try {
					supportedMethods.add(HTTPMethod.valueOf(methodName));

				} catch (IllegalArgumentException e) {
					throw new PropertyParseException("Bad HTTP method name in property cors.allowMethods: " + methodName);
				}
			}
			

			// Parse the supported headers list

			String[] headers = parseWords(pr.getOptString("cors.supportedHeaders", ""));

			supportedHeaders = new HashSet<HeaderFieldName>();

			for (String header: headers) {

				try {
					supportedHeaders.add(new HeaderFieldName(header));

				} catch (IllegalArgumentException e) {
					throw new PropertyParseException("Bad header field name in property cors.supportedHeaders: " + header);
				}
			}


			// Parse the exposed headers list
			
			headers = parseWords(pr.getOptString("cors.exposedHeaders", ""));

			exposedHeaders = new HashSet<HeaderFieldName>();

			for (String header: headers) {

				try {
					exposedHeaders.add(new HeaderFieldName(header));

				} catch (IllegalArgumentException e) {
					throw new PropertyParseException("Bad header field name in property cors.exposedHeaders: " + header);
				}
			}


			// Parse the allow credentials option
			supportsCredentials = pr.getOptBoolean("cors.supportsCredentials", true);


			// Parse the max cache age of preflight requests
			maxAge = pr.getOptInt("cors.maxAge", -1);
			
		
		} catch (PropertyParseException e) {
			
			// Simply reformat as a more specific exception class
			// to improve stack trace clarity (config exceptions
			// are often dumped to the web client screen)
			
			throw new CORSConfigurationException(e.getMessage());
		}
	}
}
