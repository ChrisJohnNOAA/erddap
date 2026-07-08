package com.cohort.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

/** A helper class for finding and splitting links in text using autolink-java. */
public class LinkHelper {

  private static final LinkExtractor EXTRACTOR =
      LinkExtractor.builder().linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW)).build();

  /** Represents a part of a string, which can be either plain text or a URL. */
  public static class LinkPart {
    public final String text;
    public final boolean isUrl;

    public LinkPart(String text, boolean isUrl) {
      this.text = text;
      this.isUrl = isUrl;
    }

    @Override
    public String toString() {
      return (isUrl ? "URL(" : "TEXT(") + text + ")";
    }
  }

  /**
   * A CharSequence that lazily replaces backslashes with forward slashes. This allows autolink-java
   * to recognize protocols like http:\\\\ as http://.
   */
  private static class NormalizedCharSequence implements CharSequence {
    private final String source;

    NormalizedCharSequence(String source) {
      this.source = source;
    }

    @Override
    public int length() {
      return source.length();
    }

    @Override
    public char charAt(int index) {
      char c = source.charAt(index);
      return c == '\\' ? '/' : c;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return new NormalizedCharSequence(source.substring(start, end));
    }

    @Override
    public String toString() {
      return source.replace('\\', '/');
    }
  }

  private static boolean isValidLink(CharSequence searchIn, LinkSpan span) {
    if (span.getType() == LinkType.WWW) {
      return true;
    }
    // LinkType.URL
    int begin = span.getBeginIndex();
    int end = span.getEndIndex();

    // Work on normalized text for validation
    String linkText = searchIn.subSequence(begin, end).toString().toLowerCase();

    // Find "://" (searchIn is already normalized, so backslashes are forward slashes)
    int schemeEnd = linkText.indexOf("://");
    if (schemeEnd == -1) return false;

    // Check if it's file:
    if (schemeEnd == 4 && linkText.startsWith("file")) {
      return true;
    }

    int hostStart = schemeEnd + 3;
    // Skip extra slashes (e.g. http:////)
    while (hostStart < linkText.length() && linkText.charAt(hostStart) == '/') {
      hostStart++;
    }
    if (hostStart >= linkText.length()) return false;

    // Check for user:pass@
    int atSign = -1;
    for (int i = hostStart; i < linkText.length(); i++) {
      char c = linkText.charAt(i);
      if (c == '/' || c == '?' || c == '#') break;
      if (c == '@') {
        atSign = i;
        break;
      }
    }
    if (atSign != -1) {
      hostStart = atSign + 1;
    }
    if (hostStart >= linkText.length()) return false;

    // Check for localhost
    if (linkText.substring(hostStart).startsWith("localhost")) {
      int endOfLocalhost = hostStart + 9;
      if (endOfLocalhost == linkText.length()) return true;
      char c = linkText.charAt(endOfLocalhost);
      if (c == '/' || c == ':' || c == '?' || c == '#') return true;
    }

    // Find host end and check for dot
    boolean hasDot = false;
    for (int i = hostStart; i < linkText.length(); i++) {
      char c = linkText.charAt(i);
      if (c == '/' || c == ':' || c == '?' || c == '#') break;
      if (c == '.') hasDot = true;
    }
    return hasDot;
  }

  /**
   * Finds the first URL in the input string starting from startIndex.
   *
   * @param input the text to search
   * @param startIndex the index to start searching from
   * @return an int array with [start, end] indices, or [-1, -1] if not found.
   */
  public static int[] findUrl(String input, int startIndex) {
    if (input == null || startIndex < 0 || startIndex >= input.length()) {
      return new int[] {-1, -1};
    }

    CharSequence searchIn = input.indexOf('\\') >= 0 ? new NormalizedCharSequence(input) : input;

    Iterable<LinkSpan> spans = EXTRACTOR.extractLinks(searchIn);
    for (LinkSpan span : spans) {
      if (span.getBeginIndex() >= startIndex) {
        if (isValidLink(searchIn, span)) {
          return new int[] {span.getBeginIndex(), span.getEndIndex()};
        }
      }
    }
    return new int[] {-1, -1};
  }

  /**
   * Checks if the input string contains any URL.
   *
   * @param input the text to check
   * @return true if a URL is found
   */
  public static boolean containsUrl(String input) {
    if (input == null || input.isEmpty()) {
      return false;
    }
    CharSequence searchIn = input.indexOf('\\') >= 0 ? new NormalizedCharSequence(input) : input;
    for (LinkSpan span : EXTRACTOR.extractLinks(searchIn)) {
      if (isValidLink(searchIn, span)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Splits the input string into parts of plain text and URLs.
   *
   * @param input the text to split
   * @return a list of LinkPart objects
   */
  public static List<LinkPart> splitByLinks(String input) {
    if (input == null) {
      return null;
    }
    List<LinkPart> parts = new ArrayList<>();

    CharSequence searchIn = input.indexOf('\\') >= 0 ? new NormalizedCharSequence(input) : input;

    int lastEnd = 0;
    for (LinkSpan span : EXTRACTOR.extractLinks(searchIn)) {
      if (isValidLink(searchIn, span)) {
        if (span.getBeginIndex() > lastEnd) {
          parts.add(new LinkPart(input.substring(lastEnd, span.getBeginIndex()), false));
        }
        parts.add(new LinkPart(input.substring(span.getBeginIndex(), span.getEndIndex()), true));
        lastEnd = span.getEndIndex();
      }
    }
    if (lastEnd < input.length()) {
      parts.add(new LinkPart(input.substring(lastEnd), false));
    }
    return parts;
  }

  /**
   * Checks if any of the parts is a URL.
   *
   * @param parts the list of parts to check
   * @return true if any part is a URL
   */
  public static boolean hasUrl(List<LinkPart> parts) {
    if (parts == null) {
      return false;
    }
    for (LinkPart part : parts) {
      if (part.isUrl) {
        return true;
      }
    }
    return false;
  }

  /**
   * This is used when setting href attributes in anchor tags. Specifically this is to make sure
   * browsers know this is an absolute url and not a relative url.
   */
  public static String addHttpsForWWW(final String input) {
    if (input != null && input.startsWith("www.")) {
      return "https://" + input;
    }
    return input;
  }
}
