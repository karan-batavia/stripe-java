package com.stripe.net;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Webhook {
  public static final long DEFAULT_TOLERANCE = 300;

  /**
   * Returns an Event instance using the provided JSON payload. Throws a JsonSyntaxException if the
   * payload is not valid JSON, and a SignatureVerificationException if the signature verification
   * fails for any reason.
   *
   * @param payload the payload sent by Stripe.
   * @param sigHeader the contents of the signature header sent by Stripe.
   * @param secret secret used to generate the signature.
   * @return the Event instance
   * @throws SignatureVerificationException if the verification fails.
   */
  public static Event constructEvent(String payload, String sigHeader, String secret)
      throws SignatureVerificationException {
    return constructEvent(payload, sigHeader, secret, DEFAULT_TOLERANCE);
  }

  /**
   * Returns an Event instance using the provided JSON payload. Throws a JsonSyntaxException if the
   * payload is not valid JSON, and a SignatureVerificationException if the signature verification
   * fails for any reason.
   *
   * @param payload the payload sent by Stripe.
   * @param sigHeader the contents of the signature header sent by Stripe.
   * @param secret secret used to generate the signature.
   * @param tolerance maximum difference in seconds allowed between the header's timestamp and the
   *     current time
   * @return the Event instance
   * @throws SignatureVerificationException if the verification fails.
   */
  public static Event constructEvent(
      String payload, String sigHeader, String secret, long tolerance)
      throws SignatureVerificationException {
    return constructEvent(payload, sigHeader, secret, tolerance, null);
  }

  /**
   * Returns an Event instance using the provided JSON payload. Throws a JsonSyntaxException if the
   * payload is not valid JSON, and a SignatureVerificationException if the signature verification
   * fails for any reason.
   *
   * @param payload the payload sent by Stripe.
   * @param sigHeader the contents of the signature header sent by Stripe.
   * @param secret secret used to generate the signature.
   * @param tolerance maximum difference in seconds allowed between the header's timestamp and the
   *     current time
   * @param clock instance of clock if you want to use custom time instance
   * @return the Event instance
   * @throws SignatureVerificationException if the verification fails.
   */
  public static Event constructEvent(
      String payload, String sigHeader, String secret, long tolerance, Clock clock)
      throws SignatureVerificationException {
    Event event =
        StripeObject.deserializeStripeObject(
            payload, Event.class, ApiResource.getGlobalResponseGetter());
    Signature.verifyHeader(payload, sigHeader, secret, tolerance, clock);
    // StripeObjects source their raw JSON object from their last response, but constructed webhooks
    // don't have that
    // in order to make the raw object available on parsed events, we fake the response.
    if (event.getLastResponse() == null) {
      event.setLastResponse(
          new StripeResponse(200, HttpHeaders.of(Collections.emptyMap()), payload));
    }

    return event;
  }

  public static final class Signature {
    public static final String EXPECTED_SCHEME = "v1";

    /**
     * Verifies the signature header sent by Stripe. Throws a SignatureVerificationException if the
     * verification fails for any reason.
     *
     * @param payload the payload sent by Stripe.
     * @param sigHeader the contents of the signature header sent by Stripe.
     * @param secret secret used to generate the signature.
     * @param tolerance maximum difference allowed between the header's timestamp and the current
     *     time
     * @throws SignatureVerificationException if the verification fails.
     */
    public static boolean verifyHeader(
        String payload, String sigHeader, String secret, long tolerance)
        throws SignatureVerificationException {
      return verifyHeader(payload, sigHeader, secret, tolerance, null);
    }

    /**
     * Verifies the signature header sent by Stripe. Throws a SignatureVerificationException if the
     * verification fails for any reason.
     *
     * @param payload the payload sent by Stripe.
     * @param sigHeader the contents of the signature header sent by Stripe.
     * @param secret secret used to generate the signature.
     * @param tolerance maximum difference allowed between the header's timestamp and the current
     *     time
     * @param clock instance of clock if you want to use custom time instance
     * @throws SignatureVerificationException if the verification fails.
     */
    public static boolean verifyHeader(
        String payload, String sigHeader, String secret, long tolerance, Clock clock)
        throws SignatureVerificationException {
      // Get timestamp and signatures from header
      long timestamp = getTimestamp(sigHeader);
      List<String> signatures = getSignatures(sigHeader, EXPECTED_SCHEME);
      if (timestamp <= 0) {
        throw new SignatureVerificationException(
            "Unable to extract timestamp and signatures from header", sigHeader);
      }
      if (signatures.size() == 0) {
        throw new SignatureVerificationException(
            "No signatures found with expected scheme", sigHeader);
      }

      // Compute expected signature
      String signedPayload = String.format("%d.%s", timestamp, payload);
      String expectedSignature;
      try {
        expectedSignature = computeSignature(signedPayload, secret);
      } catch (Exception e) {
        throw new SignatureVerificationException(
            "Unable to compute signature for payload", sigHeader);
      }

      // Check if expected signature is found in list of header's signatures
      boolean signatureFound = false;
      for (String signature : signatures) {
        if (StringUtils.secureCompare(expectedSignature, signature)) {
          signatureFound = true;
          break;
        }
      }
      if (!signatureFound) {
        throw new SignatureVerificationException(
            "No signatures found matching the expected signature for payload", sigHeader);
      }

      long currentTime = clock == null ? Util.getTimeNow() : clock.millis() / 1000;

      // Check tolerance
      if ((tolerance > 0) && (timestamp < (currentTime - tolerance))) {
        throw new SignatureVerificationException("Timestamp outside the tolerance zone", sigHeader);
      }

      return true;
    }

    /**
     * Extracts the timestamp in a signature header.
     *
     * @param sigHeader the signature header
     * @return the timestamp contained in the header.
     */
    private static long getTimestamp(String sigHeader) {
      String[] items = sigHeader.split(",", -1);

      for (String item : items) {
        String[] itemParts = item.split("=", 2);
        if (itemParts[0].equals("t")) {
          return Long.parseLong(itemParts[1]);
        }
      }

      return -1;
    }

    /**
     * Extracts the signatures matching a given scheme in a signature header.
     *
     * @param sigHeader the signature header
     * @param scheme the signature scheme to look for.
     * @return the list of signatures matching the provided scheme.
     */
    private static List<String> getSignatures(String sigHeader, String scheme) {
      List<String> signatures = new ArrayList<String>();
      String[] items = sigHeader.split(",", -1);

      for (String item : items) {
        String[] itemParts = item.split("=", 2);
        if (itemParts[0].equals(scheme)) {
          signatures.add(itemParts[1]);
        }
      }

      return signatures;
    }

    /**
     * Computes the signature for a given payload and secret.
     *
     * <p>The current scheme used by Stripe ("v1") is HMAC/SHA-256.
     *
     * @param payload the payload to sign.
     * @param secret the secret used to generate the signature.
     * @return the signature as a string.
     */
    private static String computeSignature(String payload, String secret)
        throws NoSuchAlgorithmException, InvalidKeyException {
      return Util.computeHmacSha256(secret, payload);
    }
  }

  public static final class Util {
    /**
     * Computes the HMAC/SHA-256 code for a given key and message.
     *
     * @param key the key used to generate the code.
     * @param message the message.
     * @return the code as a string.
     */
    public static String computeHmacSha256(String key, String message)
        throws NoSuchAlgorithmException, InvalidKeyException {
      Mac hasher = Mac.getInstance("HmacSHA256");
      hasher.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = hasher.doFinal(message.getBytes(StandardCharsets.UTF_8));
      String result = "";
      for (byte b : hash) {
        result += Integer.toString((b & 0xff) + 0x100, 16).substring(1);
      }
      return result;
    }

    /**
     * Returns the current UTC timestamp in seconds.
     *
     * @return the timestamp as a long.
     */
    public static long getTimeNow() {
      long time = System.currentTimeMillis() / 1000L;
      return time;
    }
  }
}
