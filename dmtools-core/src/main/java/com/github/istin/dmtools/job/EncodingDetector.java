package com.github.istin.dmtools.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for detecting and decoding encoded parameters.
 * Supports auto-detection of base64 and URL encoding formats.
 */
public class EncodingDetector {
    
    private static final Logger logger = LogManager.getLogger(EncodingDetector.class);
    
    /**
     * Auto-detects encoding format and decodes the input string.
     * Attempts base64 decoding first, falls back to URL decoding if base64 fails.
     * 
     * @param encoded The encoded string to decode
     * @return The decoded string
     * @throws IllegalArgumentException if neither base64 nor URL decoding succeeds
     */
    public String autoDetectAndDecode(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoded parameter cannot be null or empty");
        }
        
        String trimmedEncoded = encoded.trim();
        
        try {
            // Try base64 decoding first
            String decoded = decodeBase64(trimmedEncoded);
            if (decoded == null || decoded.trim().isEmpty()) {
                logger.warn("Base64 decoding succeeded but produced empty result, attempting URL decoding");
                throw new IllegalArgumentException("Base64 decoding produced empty result");
            }
            // Unescape quotes if present (handles cases where JSON was escaped before encoding)
            String unescaped = unescapeJsonQuotes(decoded);
            logger.info("Successfully decoded parameter using base64 encoding");
            return unescaped.trim();
        } catch (Exception e) {
            logger.debug("Base64 decoding failed, attempting URL decoding: {}", e.getMessage());
            
            try {
                // Fallback to URL decoding
                String decoded = decodeUrl(trimmedEncoded);
                if (decoded == null || decoded.trim().isEmpty()) {
                    logger.error("URL decoding succeeded but produced empty result");
                    throw new IllegalArgumentException(
                        "URL decoding produced empty result. " +
                        "The encoded parameter may be empty or contain only whitespace."
                    );
                }
                // Unescape quotes if present (handles cases where JSON was escaped before encoding)
                String unescaped = unescapeJsonQuotes(decoded);
                logger.info("Successfully decoded parameter using URL encoding");
                return unescaped.trim();
            } catch (Exception urlException) {
                logger.error("Both base64 and URL decoding failed for input parameter");
                throw new IllegalArgumentException(
                    "Unable to decode parameter - neither base64 nor URL encoding format detected. " +
                    "Base64 error: " + e.getMessage() + ". URL error: " + urlException.getMessage() + ". " +
                    "Input preview: " + (trimmedEncoded.length() > 50 ? trimmedEncoded.substring(0, 50) + "..." : trimmedEncoded)
                );
            }
        }
    }
    
    /**
     * Decodes a base64-encoded string.
     * Leverages existing JobRunner.decodeBase64() method for consistency.
     * 
     * @param input The base64-encoded string
     * @return The decoded string
     * @throws IllegalArgumentException if base64 decoding fails
     */
    public String decodeBase64(String input) {
        try {
            return JobRunner.decodeBase64(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid base64 encoding: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decodes a URL-encoded string.
     * 
     * @param input The URL-encoded string
     * @return The decoded string
     * @throws IllegalArgumentException if URL decoding fails
     */
    public String decodeUrl(String input) {
        try {
            return URLDecoder.decode(input, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL encoding: " + e.getMessage(), e);
        }
    }
    
    /**
     * Unescapes JSON quotes in a string.
     * Converts escaped quotes (\") to actual quotes (").
     * This handles cases where JSON was escaped before being URL encoded.
     * 
     * @param input The string that may contain escaped quotes
     * @return The string with unescaped quotes
     */
    private String unescapeJsonQuotes(String input) {
        if (input == null) {
            return null;
        }
        // Replace \" with " to fix escaped quotes
        String unescaped = input.replace("\\\"", "\"");
        // Also handle other common escape sequences that might appear
        unescaped = unescaped.replace("\\n", "\n");
        unescaped = unescaped.replace("\\r", "\r");
        unescaped = unescaped.replace("\\t", "\t");
        return unescaped;
    }
}
