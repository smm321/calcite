/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.test;

import org.apache.calcite.avatica.util.ByteString;
import org.apache.calcite.avatica.util.DateTimeUtils;
import org.apache.calcite.runtime.CalciteException;
import org.apache.calcite.runtime.SqlFunctions;
import org.apache.calcite.runtime.Utilities;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.apache.calcite.avatica.util.DateTimeUtils.MILLIS_PER_DAY;
import static org.apache.calcite.avatica.util.DateTimeUtils.dateStringToUnixDate;
import static org.apache.calcite.avatica.util.DateTimeUtils.timeStringToUnixDate;
import static org.apache.calcite.avatica.util.DateTimeUtils.timestampStringToUnixDate;
import static org.apache.calcite.runtime.SqlFunctions.charLength;
import static org.apache.calcite.runtime.SqlFunctions.concat;
import static org.apache.calcite.runtime.SqlFunctions.fromBase64;
import static org.apache.calcite.runtime.SqlFunctions.greater;
import static org.apache.calcite.runtime.SqlFunctions.initcap;
import static org.apache.calcite.runtime.SqlFunctions.internalToDate;
import static org.apache.calcite.runtime.SqlFunctions.internalToTime;
import static org.apache.calcite.runtime.SqlFunctions.internalToTimestamp;
import static org.apache.calcite.runtime.SqlFunctions.lesser;
import static org.apache.calcite.runtime.SqlFunctions.lower;
import static org.apache.calcite.runtime.SqlFunctions.ltrim;
import static org.apache.calcite.runtime.SqlFunctions.md5;
import static org.apache.calcite.runtime.SqlFunctions.posixRegex;
import static org.apache.calcite.runtime.SqlFunctions.regexpReplace;
import static org.apache.calcite.runtime.SqlFunctions.rtrim;
import static org.apache.calcite.runtime.SqlFunctions.sha1;
import static org.apache.calcite.runtime.SqlFunctions.sha256;
import static org.apache.calcite.runtime.SqlFunctions.sha512;
import static org.apache.calcite.runtime.SqlFunctions.toBase64;
import static org.apache.calcite.runtime.SqlFunctions.toChar;
import static org.apache.calcite.runtime.SqlFunctions.toInt;
import static org.apache.calcite.runtime.SqlFunctions.toIntOptional;
import static org.apache.calcite.runtime.SqlFunctions.toLong;
import static org.apache.calcite.runtime.SqlFunctions.toLongOptional;
import static org.apache.calcite.runtime.SqlFunctions.trim;
import static org.apache.calcite.runtime.SqlFunctions.upper;
import static org.apache.calcite.test.Matchers.within;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Unit test for the methods in {@link SqlFunctions} that implement SQL
 * functions.
 *
 * <p>Developers, please use {@link org.hamcrest.MatcherAssert#assertThat assertThat}
 * rather than {@code assertEquals}.
 */
class SqlFunctionsTest {
  static <E> List<E> list(E... es) {
    return Arrays.asList(es);
  }

  static <E> List<E> list() {
    return ImmutableList.of();
  }

  @Test void testCharLength() {
    assertThat(charLength("xyz"), is(3));
  }

  @Test void testToString() {
    assertThat(SqlFunctions.toString(0f), is("0E0"));
    assertThat(SqlFunctions.toString(1f), is("1"));
    assertThat(SqlFunctions.toString(1.5f), is("1.5"));
    assertThat(SqlFunctions.toString(-1.5f), is("-1.5"));
    assertThat(SqlFunctions.toString(1.5e8f), is("1.5E8"));
    assertThat(SqlFunctions.toString(-0.0625f), is("-0.0625"));
    assertThat(SqlFunctions.toString(0.0625f), is("0.0625"));
    assertThat(SqlFunctions.toString(-5e-12f), is("-5E-12"));

    assertThat(SqlFunctions.toString(0d), is("0E0"));
    assertThat(SqlFunctions.toString(1d), is("1"));
    assertThat(SqlFunctions.toString(1.5d), is("1.5"));
    assertThat(SqlFunctions.toString(-1.5d), is("-1.5"));
    assertThat(SqlFunctions.toString(1.5e8d), is("1.5E8"));
    assertThat(SqlFunctions.toString(-0.0625d), is("-0.0625"));
    assertThat(SqlFunctions.toString(0.0625d), is("0.0625"));
    assertThat(SqlFunctions.toString(-5e-12d), is("-5E-12"));

    assertThat(SqlFunctions.toString(new BigDecimal("0")), is("0"));
    assertThat(SqlFunctions.toString(new BigDecimal("1")), is("1"));
    assertThat(SqlFunctions.toString(new BigDecimal("1.5")), is("1.5"));
    assertThat(SqlFunctions.toString(new BigDecimal("-1.5")), is("-1.5"));
    assertThat(SqlFunctions.toString(new BigDecimal("1.5e8")), is("1.5E+8"));
    assertThat(SqlFunctions.toString(new BigDecimal("-0.0625")), is("-.0625"));
    assertThat(SqlFunctions.toString(new BigDecimal("0.0625")), is(".0625"));
    assertThat(SqlFunctions.toString(new BigDecimal("-5e-12")), is("-5E-12"));
  }

  @Test void testConcat() {
    assertThat(concat("a b", "cd"), is("a bcd"));
    // The code generator will ensure that nulls are never passed in. If we
    // pass in null, it is treated like the string "null", as the following
    // tests show. Not the desired behavior for SQL.
    assertThat(concat("a", null), is("anull"));
    assertThat(concat((String) null, null), is("nullnull"));
    assertThat(concat(null, "b"), is("nullb"));
  }

  @Test void testPosixRegex() {
    assertThat(posixRegex("abc", "abc", true), is(true));
    assertThat(posixRegex("abc", "^a", true), is(true));
    assertThat(posixRegex("abc", "(b|d)", true), is(true));
    assertThat(posixRegex("abc", "^(b|c)", true), is(false));

    assertThat(posixRegex("abc", "ABC", false), is(true));
    assertThat(posixRegex("abc", "^A", false), is(true));
    assertThat(posixRegex("abc", "(B|D)", false), is(true));
    assertThat(posixRegex("abc", "^(B|C)", false), is(false));

    assertThat(posixRegex("abc", "^[[:xdigit:]]$", false), is(false));
    assertThat(posixRegex("abc", "^[[:xdigit:]]+$", false), is(true));
    assertThat(posixRegex("abcq", "^[[:xdigit:]]+$", false), is(false));

    assertThat(posixRegex("abc", "[[:xdigit:]]", false), is(true));
    assertThat(posixRegex("abc", "[[:xdigit:]]+", false), is(true));
    assertThat(posixRegex("abcq", "[[:xdigit:]]", false), is(true));
  }

  @Test void testRegexpReplace() {
    assertThat(regexpReplace("a b c", "b", "X"), is("a X c"));
    assertThat(regexpReplace("abc def ghi", "[g-z]+", "X"), is("abc def X"));
    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X"), is("X X X"));
    assertThat(regexpReplace("a b c", "a|b", "X"), is("X X c"));
    assertThat(regexpReplace("a b c", "y", "X"), is("a b c"));

    assertThat(regexpReplace("100-200", "(\\d+)", "num"), is("num-num"));
    assertThat(regexpReplace("100-200", "(\\d+)", "###"), is("###-###"));
    assertThat(regexpReplace("100-200", "(-)", "###"), is("100###200"));

    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X", 1), is("X X X"));
    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X", 2), is("aX X X"));
    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X", 1, 3),
        is("abc def X"));
    assertThat(regexpReplace("abc def GHI", "[a-z]+", "X", 1, 3, "c"),
        is("abc def GHI"));
    assertThat(regexpReplace("abc def GHI", "[a-z]+", "X", 1, 3, "i"),
        is("abc def X"));

    try {
      regexpReplace("abc def ghi", "[a-z]+", "X", 0);
      fail("'regexp_replace' on an invalid pos is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid input for REGEXP_REPLACE: '0'"));
    }

    try {
      regexpReplace("abc def ghi", "[a-z]+", "X", 1, 3, "WWW");
      fail("'regexp_replace' on an invalid matchType is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid input for REGEXP_REPLACE: 'WWW'"));
    }
  }

  @Test void testLower() {
    assertThat(lower("A bCd Iijk"), is("a bcd iijk"));
  }

  @Test void testFromBase64() {
    final List<String> expectedList =
        Arrays.asList("", "\0", "0", "a", " ", "\n", "\r\n", "\u03C0",
            "hello\tword");
    for (String expected : expectedList) {
      assertThat(fromBase64(toBase64(expected)),
          is(new ByteString(expected.getBytes(UTF_8))));
    }
    assertThat("546869732069732061207465737420537472696e672e",
        is(fromBase64("VGhpcyB  pcyBh\rIHRlc3Qg\tU3Ry\naW5nLg==").toString()));
    assertThat(fromBase64("-1"), nullValue());
  }

  @Test void testToBase64() {
    final String s = ""
        + "This is a test String. check resulte out of 76This is a test String."
        + "This is a test String.This is a test String.This is a test String."
        + "This is a test String. This is a test String. check resulte out of 76"
        + "This is a test String.This is a test String.This is a test String."
        + "This is a test String.This is a test String. This is a test String. "
        + "check resulte out of 76This is a test String.This is a test String."
        + "This is a test String.This is a test String.This is a test String.";
    final String actual = ""
        + "VGhpcyBpcyBhIHRlc3QgU3RyaW5nLiBjaGVjayByZXN1bHRlIG91dCBvZiA3NlRoaXMgaXMgYSB0\n"
        + "ZXN0IFN0cmluZy5UaGlzIGlzIGEgdGVzdCBTdHJpbmcuVGhpcyBpcyBhIHRlc3QgU3RyaW5nLlRo\n"
        + "aXMgaXMgYSB0ZXN0IFN0cmluZy5UaGlzIGlzIGEgdGVzdCBTdHJpbmcuIFRoaXMgaXMgYSB0ZXN0\n"
        + "IFN0cmluZy4gY2hlY2sgcmVzdWx0ZSBvdXQgb2YgNzZUaGlzIGlzIGEgdGVzdCBTdHJpbmcuVGhp\n"
        + "cyBpcyBhIHRlc3QgU3RyaW5nLlRoaXMgaXMgYSB0ZXN0IFN0cmluZy5UaGlzIGlzIGEgdGVzdCBT\n"
        + "dHJpbmcuVGhpcyBpcyBhIHRlc3QgU3RyaW5nLiBUaGlzIGlzIGEgdGVzdCBTdHJpbmcuIGNoZWNr\n"
        + "IHJlc3VsdGUgb3V0IG9mIDc2VGhpcyBpcyBhIHRlc3QgU3RyaW5nLlRoaXMgaXMgYSB0ZXN0IFN0\n"
        + "cmluZy5UaGlzIGlzIGEgdGVzdCBTdHJpbmcuVGhpcyBpcyBhIHRlc3QgU3RyaW5nLlRoaXMgaXMg\n"
        + "YSB0ZXN0IFN0cmluZy4=";
    assertThat(toBase64(s), is(actual));
    assertThat(toBase64(""), is(""));
  }

  @Test void testUpper() {
    assertThat(upper("A bCd iIjk"), is("A BCD IIJK"));
  }

  @Test void testInitcap() {
    assertThat(initcap("aA"), is("Aa"));
    assertThat(initcap("zz"), is("Zz"));
    assertThat(initcap("AZ"), is("Az"));
    assertThat(initcap("tRy a littlE  "), is("Try A Little  "));
    assertThat(initcap("won't it?no"), is("Won'T It?No"));
    assertThat(initcap("1A"), is("1a"));
    assertThat(initcap(" b0123B"), is(" B0123b"));
  }

  @Test void testLesser() {
    assertThat(lesser("a", "bc"), is("a"));
    assertThat(lesser("bc", "ac"), is("ac"));
    try {
      Object o = lesser("a", null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
    assertThat(lesser(null, "a"), is("a"));
    assertThat(lesser((String) null, null), nullValue());
  }

  @Test void testGreater() {
    assertThat(greater("a", "bc"), is("bc"));
    assertThat(greater("bc", "ac"), is("bc"));
    try {
      Object o = greater("a", null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
    assertThat(greater(null, "a"), is("a"));
    assertThat(greater((String) null, null), nullValue());
  }

  /** Test for {@link SqlFunctions#rtrim}. */
  @Test void testRtrim() {
    assertThat(rtrim(""), is(""));
    assertThat(rtrim("    "), is(""));
    assertThat(rtrim("   x  "), is("   x"));
    assertThat(rtrim("   x "), is("   x"));
    assertThat(rtrim("   x y "), is("   x y"));
    assertThat(rtrim("   x"), is("   x"));
    assertThat(rtrim("x"), is("x"));
  }

  /** Test for {@link SqlFunctions#ltrim}. */
  @Test void testLtrim() {
    assertThat(ltrim(""), is(""));
    assertThat(ltrim("    "), is(""));
    assertThat(ltrim("   x  "), is("x  "));
    assertThat(ltrim("   x "), is("x "));
    assertThat(ltrim("x y "), is("x y "));
    assertThat(ltrim("   x"), is("x"));
    assertThat(ltrim("x"), is("x"));
  }

  /** Test for {@link SqlFunctions#trim}. */
  @Test void testTrim() {
    assertThat(trimSpacesBoth(""), is(""));
    assertThat(trimSpacesBoth("    "), is(""));
    assertThat(trimSpacesBoth("   x  "), is("x"));
    assertThat(trimSpacesBoth("   x "), is("x"));
    assertThat(trimSpacesBoth("   x y "), is("x y"));
    assertThat(trimSpacesBoth("   x"), is("x"));
    assertThat(trimSpacesBoth("x"), is("x"));
  }

  static String trimSpacesBoth(String s) {
    return trim(true, true, " ", s);
  }

  @Test void testFloor() {
    checkFloor(0, 10, 0);
    checkFloor(27, 10, 20);
    checkFloor(30, 10, 30);
    checkFloor(-30, 10, -30);
    checkFloor(-27, 10, -30);
  }

  private void checkFloor(int x, int y, int result) {
    assertThat(SqlFunctions.floor(x, y), is(result));
    assertThat(SqlFunctions.floor((long) x, (long) y), is((long) result));
    assertThat(SqlFunctions.floor((short) x, (short) y), is((short) result));
    assertThat(SqlFunctions.floor((byte) x, (byte) y), is((byte) result));
    assertThat(
        SqlFunctions.floor(BigDecimal.valueOf(x), BigDecimal.valueOf(y)),
        is(BigDecimal.valueOf(result)));
  }

  @Test void testCeil() {
    checkCeil(0, 10, 0);
    checkCeil(27, 10, 30);
    checkCeil(30, 10, 30);
    checkCeil(-30, 10, -30);
    checkCeil(-27, 10, -20);
    checkCeil(-27, 1, -27);
  }

  private void checkCeil(int x, int y, int result) {
    assertThat(SqlFunctions.ceil(x, y), is(result));
    assertThat(SqlFunctions.ceil((long) x, (long) y), is((long) result));
    assertThat(SqlFunctions.ceil((short) x, (short) y), is((short) result));
    assertThat(SqlFunctions.ceil((byte) x, (byte) y), is((byte) result));
    assertThat(
        SqlFunctions.ceil(BigDecimal.valueOf(x), BigDecimal.valueOf(y)),
        is(BigDecimal.valueOf(result)));
  }

  /** Unit test for
   * {@link Utilities#compare(java.util.List, java.util.List)}. */
  @Test void testCompare() {
    final List<String> ac = Arrays.asList("a", "c");
    final List<String> abc = Arrays.asList("a", "b", "c");
    final List<String> a = Collections.singletonList("a");
    final List<String> empty = Collections.emptyList();
    assertThat(Utilities.compare(ac, ac), is(0));
    assertThat(Utilities.compare(ac, new ArrayList<>(ac)), is(0));
    assertThat(Utilities.compare(a, ac), is(-1));
    assertThat(Utilities.compare(empty, ac), is(-1));
    assertThat(Utilities.compare(ac, a), is(1));
    assertThat(Utilities.compare(ac, abc), is(1));
    assertThat(Utilities.compare(ac, empty), is(1));
    assertThat(Utilities.compare(empty, empty), is(0));
  }

  @Test void testTruncateLong() {
    assertThat(SqlFunctions.truncate(12345L, 1000L), is(12000L));
    assertThat(SqlFunctions.truncate(12000L, 1000L), is(12000L));
    assertThat(SqlFunctions.truncate(12001L, 1000L), is(12000L));
    assertThat(SqlFunctions.truncate(11999L, 1000L), is(11000L));

    assertThat(SqlFunctions.truncate(-12345L, 1000L), is(-13000L));
    assertThat(SqlFunctions.truncate(-12000L, 1000L), is(-12000L));
    assertThat(SqlFunctions.truncate(-12001L, 1000L), is(-13000L));
    assertThat(SqlFunctions.truncate(-11999L, 1000L), is(-12000L));
  }

  @Test void testTruncateInt() {
    assertThat(SqlFunctions.truncate(12345, 1000), is(12000));
    assertThat(SqlFunctions.truncate(12000, 1000), is(12000));
    assertThat(SqlFunctions.truncate(12001, 1000), is(12000));
    assertThat(SqlFunctions.truncate(11999, 1000), is(11000));

    assertThat(SqlFunctions.truncate(-12345, 1000), is(-13000));
    assertThat(SqlFunctions.truncate(-12000, 1000), is(-12000));
    assertThat(SqlFunctions.truncate(-12001, 1000), is(-13000));
    assertThat(SqlFunctions.truncate(-11999, 1000), is(-12000));

    assertThat(SqlFunctions.round(12345, 1000), is(12000));
    assertThat(SqlFunctions.round(12845, 1000), is(13000));
    assertThat(SqlFunctions.round(-12345, 1000), is(-12000));
    assertThat(SqlFunctions.round(-12845, 1000), is(-13000));
  }

  @Test void testSTruncateDouble() {
    assertThat(SqlFunctions.struncate(12.345d, 3), within(12.345d, 0.001));
    assertThat(SqlFunctions.struncate(12.345d, 2), within(12.340d, 0.001));
    assertThat(SqlFunctions.struncate(12.345d, 1), within(12.300d, 0.001));
    assertThat(SqlFunctions.struncate(12.999d, 0), within(12.000d, 0.001));

    assertThat(SqlFunctions.struncate(-12.345d, 3), within(-12.345d, 0.001));
    assertThat(SqlFunctions.struncate(-12.345d, 2), within(-12.340d, 0.001));
    assertThat(SqlFunctions.struncate(-12.345d, 1), within(-12.300d, 0.001));
    assertThat(SqlFunctions.struncate(-12.999d, 0), within(-12.000d, 0.001));

    assertThat(SqlFunctions.struncate(12345d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12001d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000d, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.struncate(12000d, -5), within(0d, 0.001));
    assertThat(SqlFunctions.struncate(11999d, -3), within(11000d, 0.001));

    assertThat(SqlFunctions.struncate(-12345d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-11999d, -3), within(-11000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000d, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000d, -5), within(0d, 0.001));
  }

  @Test void testSTruncateLong() {
    assertThat(SqlFunctions.struncate(12345L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12001L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000L, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.struncate(12000L, -5), within(0d, 0.001));
    assertThat(SqlFunctions.struncate(11999L, -3), within(11000d, 0.001));

    assertThat(SqlFunctions.struncate(-12345L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-11999L, -3), within(-11000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000L, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000L, -5), within(0d, 0.001));
  }

  @Test void testSTruncateInt() {
    assertThat(SqlFunctions.struncate(12345, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12001, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.struncate(12000, -5), within(0d, 0.001));
    assertThat(SqlFunctions.struncate(11999, -3), within(11000d, 0.001));

    assertThat(SqlFunctions.struncate(-12345, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-11999, -3), within(-11000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000, -5), within(0d, 0.001));
  }

  @Test void testSRoundDouble() {
    assertThat(SqlFunctions.sround(12.345d, 3), within(12.345d, 0.001));
    assertThat(SqlFunctions.sround(12.345d, 2), within(12.350d, 0.001));
    assertThat(SqlFunctions.sround(12.345d, 1), within(12.300d, 0.001));
    assertThat(SqlFunctions.sround(12.999d, 2), within(13.000d, 0.001));
    assertThat(SqlFunctions.sround(12.999d, 1), within(13.000d, 0.001));
    assertThat(SqlFunctions.sround(12.999d, 0), within(13.000d, 0.001));

    assertThat(SqlFunctions.sround(-12.345d, 3), within(-12.345d, 0.001));
    assertThat(SqlFunctions.sround(-12.345d, 2), within(-12.350d, 0.001));
    assertThat(SqlFunctions.sround(-12.345d, 1), within(-12.300d, 0.001));
    assertThat(SqlFunctions.sround(-12.999d, 2), within(-13.000d, 0.001));
    assertThat(SqlFunctions.sround(-12.999d, 1), within(-13.000d, 0.001));
    assertThat(SqlFunctions.sround(-12.999d, 0), within(-13.000d, 0.001));

    assertThat(SqlFunctions.sround(12345d, -1), within(12350d, 0.001));
    assertThat(SqlFunctions.sround(12345d, -2), within(12300d, 0.001));
    assertThat(SqlFunctions.sround(12345d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12001d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000d, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.sround(12000d, -5), within(0d, 0.001));
    assertThat(SqlFunctions.sround(11999d, -3), within(12000d, 0.001));

    assertThat(SqlFunctions.sround(-12345d, -1), within(-12350d, 0.001));
    assertThat(SqlFunctions.sround(-12345d, -2), within(-12300d, 0.001));
    assertThat(SqlFunctions.sround(-12345d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-11999d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000d, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.sround(-12000d, -5), within(0d, 0.001));
  }

  @Test void testSRoundLong() {
    assertThat(SqlFunctions.sround(12345L, -1), within(12350d, 0.001));
    assertThat(SqlFunctions.sround(12345L, -2), within(12300d, 0.001));
    assertThat(SqlFunctions.sround(12345L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12001L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000L, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.sround(12000L, -5), within(0d, 0.001));
    assertThat(SqlFunctions.sround(11999L, -3), within(12000d, 0.001));

    assertThat(SqlFunctions.sround(-12345L, -1), within(-12350d, 0.001));
    assertThat(SqlFunctions.sround(-12345L, -2), within(-12300d, 0.001));
    assertThat(SqlFunctions.sround(-12345L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-11999L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000L, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.sround(-12000L, -5), within(0d, 0.001));
  }

  @Test void testSRoundInt() {
    assertThat(SqlFunctions.sround(12345, -1), within(12350d, 0.001));
    assertThat(SqlFunctions.sround(12345, -2), within(12300d, 0.001));
    assertThat(SqlFunctions.sround(12345, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12001, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.sround(12000, -5), within(0d, 0.001));
    assertThat(SqlFunctions.sround(11999, -3), within(12000d, 0.001));

    assertThat(SqlFunctions.sround(-12345, -1), within(-12350d, 0.001));
    assertThat(SqlFunctions.sround(-12345, -2), within(-12300d, 0.001));
    assertThat(SqlFunctions.sround(-12345, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-11999, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.sround(-12000, -5), within(0d, 0.001));
  }

  @Test void testSplit() {
    assertThat("no occurrence of delimiter",
        SqlFunctions.split("abc", ","), is(list("abc")));
    assertThat("delimiter in middle",
        SqlFunctions.split("abc", "b"), is(list("a", "c")));
    assertThat("delimiter at end",
        SqlFunctions.split("abc", "c"), is(list("ab", "")));
    assertThat("delimiter at start",
        SqlFunctions.split("abc", "a"), is(list("", "bc")));
    assertThat("empty delimiter",
        SqlFunctions.split("abc", ""), is(list("abc")));
    assertThat("empty delimiter and string",
        SqlFunctions.split("", ""), is(list()));
    assertThat("empty string",
        SqlFunctions.split("", ","), is(list()));
    assertThat("long delimiter (occurs at start)",
        SqlFunctions.split("abracadabra", "ab"), is(list("", "racad", "ra")));
    assertThat("long delimiter (occurs at end)",
        SqlFunctions.split("sabracadabrab", "ab"),
        is(list("s", "racad", "r", "")));

    // Same as above but for ByteString
    final ByteString a = ByteString.of("aa", 16);
    final ByteString ab = ByteString.of("aabb", 16);
    final ByteString abc = ByteString.of("aabbcc", 16);
    final ByteString abracadabra = ByteString.of("aabb44aaccaaddaabb44aa", 16);
    final ByteString b = ByteString.of("bb", 16);
    final ByteString bc = ByteString.of("bbcc", 16);
    final ByteString c = ByteString.of("cc", 16);
    final ByteString f = ByteString.of("ff", 16);
    final ByteString r = ByteString.of("44", 16);
    final ByteString ra = ByteString.of("44aa", 16);
    final ByteString racad = ByteString.of("44aaccaadd", 16);
    final ByteString empty = ByteString.of("", 16);
    final ByteString s = ByteString.of("55", 16);
    final ByteString sabracadabrab =
        ByteString.of("55", 16).concat(abracadabra).concat(b);
    assertThat("no occurrence of delimiter",
        SqlFunctions.split(abc, f), is(list(abc)));
    assertThat("delimiter in middle",
        SqlFunctions.split(abc, b), is(list(a, c)));
    assertThat("delimiter at end",
        SqlFunctions.split(abc, c), is(list(ab, empty)));
    assertThat("delimiter at start",
        SqlFunctions.split(abc, a), is(list(empty, bc)));
    assertThat("empty delimiter",
        SqlFunctions.split(abc, empty), is(list(abc)));
    assertThat("empty delimiter and string",
        SqlFunctions.split(empty, empty), is(list()));
    assertThat("empty string",
        SqlFunctions.split(empty, f), is(list()));
    assertThat("long delimiter (occurs at start)",
        SqlFunctions.split(abracadabra, ab), is(list(empty, racad, ra)));
    assertThat("long delimiter (occurs at end)",
        SqlFunctions.split(sabracadabrab, ab),
        is(list(s, racad, r, empty)));
  }

  @Test void testByteString() {
    final byte[] bytes = {(byte) 0xAB, (byte) 0xFF};
    final ByteString byteString = new ByteString(bytes);
    assertThat(byteString.length(), is(2));
    assertThat(byteString.toString(), is("abff"));
    assertThat(byteString.toString(16), is("abff"));
    assertThat(byteString.toString(2), is("1010101111111111"));

    final ByteString emptyByteString = new ByteString(new byte[0]);
    assertThat(emptyByteString.length(), is(0));
    assertThat(emptyByteString.toString(), is(""));
    assertThat(emptyByteString.toString(16), is(""));
    assertThat(emptyByteString.toString(2), is(""));

    assertThat(ByteString.EMPTY, is(emptyByteString));

    assertThat(byteString.substring(1, 2).toString(), is("ff"));
    assertThat(byteString.substring(0, 2).toString(), is("abff"));
    assertThat(byteString.substring(2, 2).toString(), is(""));

    // Add empty string, get original string back
    assertSame(byteString.concat(emptyByteString), byteString);
    final ByteString byteString1 = new ByteString(new byte[]{(byte) 12});
    assertThat(byteString.concat(byteString1).toString(), is("abff0c"));

    final byte[] bytes3 = {(byte) 0xFF};
    final ByteString byteString3 = new ByteString(bytes3);

    assertThat(byteString.indexOf(emptyByteString), is(0));
    assertThat(byteString.indexOf(byteString1), is(-1));
    assertThat(byteString.indexOf(byteString3), is(1));
    assertThat(byteString3.indexOf(byteString), is(-1));

    thereAndBack(bytes);
    thereAndBack(emptyByteString.getBytes());
    thereAndBack(new byte[]{10, 0, 29, -80});

    assertThat(ByteString.of("ab12", 16).toString(16), equalTo("ab12"));
    assertThat(ByteString.of("AB0001DdeAD3", 16).toString(16),
        equalTo("ab0001ddead3"));
    assertThat(ByteString.of("", 16), equalTo(emptyByteString));
    try {
      ByteString x = ByteString.of("ABg0", 16);
      fail("expected error, got " + x);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), equalTo("invalid hex character: g"));
    }
    try {
      ByteString x = ByteString.of("ABC", 16);
      fail("expected error, got " + x);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), equalTo("hex string has odd length"));
    }

    final byte[] bytes4 = {10, 0, 1, -80};
    final ByteString byteString4 = new ByteString(bytes4);
    final byte[] bytes5 = {10, 0, 1, 127};
    final ByteString byteString5 = new ByteString(bytes5);
    final ByteString byteString6 = new ByteString(bytes4);

    assertThat(byteString4.compareTo(byteString5) > 0, is(true));
    assertThat(byteString4.compareTo(byteString6) == 0, is(true));
    assertThat(byteString5.compareTo(byteString4) < 0, is(true));
  }

  private void thereAndBack(byte[] bytes) {
    final ByteString byteString = new ByteString(bytes);
    final byte[] bytes2 = byteString.getBytes();
    assertThat(bytes, equalTo(bytes2));

    final String base64String = byteString.toBase64String();
    final ByteString byteString1 = ByteString.ofBase64(base64String);
    assertThat(byteString, equalTo(byteString1));
  }

  @Test void testEqWithAny() {
    // Non-numeric same type equality check
    assertThat(SqlFunctions.eqAny("hello", "hello"), is(true));

    // Numeric types equality check
    assertThat(SqlFunctions.eqAny(1, 1L), is(true));
    assertThat(SqlFunctions.eqAny(1, 1.0D), is(true));
    assertThat(SqlFunctions.eqAny(1L, 1.0D), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), 1), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), 1L), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), 1.0D), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type equality check
    assertThat(SqlFunctions.eqAny("2", 2), is(false));
  }

  @Test void testNeWithAny() {
    // Non-numeric same type inequality check
    assertThat(SqlFunctions.neAny("hello", "world"), is(true));

    // Numeric types inequality check
    assertThat(SqlFunctions.neAny(1, 2L), is(true));
    assertThat(SqlFunctions.neAny(1, 2.0D), is(true));
    assertThat(SqlFunctions.neAny(1L, 2.0D), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), 1), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), 1L), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), 1.0D), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type inequality check
    assertThat(SqlFunctions.neAny("2", 2), is(true));
  }

  @Test void testLtWithAny() {
    // Non-numeric same type "less then" check
    assertThat(SqlFunctions.ltAny("apple", "banana"), is(true));

    // Numeric types "less than" check
    assertThat(SqlFunctions.ltAny(1, 2L), is(true));
    assertThat(SqlFunctions.ltAny(1, 2.0D), is(true));
    assertThat(SqlFunctions.ltAny(1L, 2.0D), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), 2), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), 2L), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), 2.0D), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), new BigDecimal(2.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "less than" check
    try {
      assertThat(SqlFunctions.ltAny("1", 2L), is(false));
      fail("'lt' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String < "
              + "class java.lang.Long"));
    }
  }

  @Test void testLeWithAny() {
    // Non-numeric same type "less or equal" check
    assertThat(SqlFunctions.leAny("apple", "banana"), is(true));
    assertThat(SqlFunctions.leAny("apple", "apple"), is(true));

    // Numeric types "less or equal" check
    assertThat(SqlFunctions.leAny(1, 2L), is(true));
    assertThat(SqlFunctions.leAny(1, 1L), is(true));
    assertThat(SqlFunctions.leAny(1, 2.0D), is(true));
    assertThat(SqlFunctions.leAny(1, 1.0D), is(true));
    assertThat(SqlFunctions.leAny(1L, 2.0D), is(true));
    assertThat(SqlFunctions.leAny(1L, 1.0D), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 2), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 1), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 2L), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 1L), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 2.0D), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 1.0D), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), new BigDecimal(2.0D)),
        is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "less or equal" check
    try {
      assertThat(SqlFunctions.leAny("2", 2L), is(false));
      fail("'le' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String <= "
              + "class java.lang.Long"));
    }
  }

  @Test void testGtWithAny() {
    // Non-numeric same type "greater then" check
    assertThat(SqlFunctions.gtAny("banana", "apple"), is(true));

    // Numeric types "greater than" check
    assertThat(SqlFunctions.gtAny(2, 1L), is(true));
    assertThat(SqlFunctions.gtAny(2, 1.0D), is(true));
    assertThat(SqlFunctions.gtAny(2L, 1.0D), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), 1), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), 1L), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), 1.0D), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "greater than" check
    try {
      assertThat(SqlFunctions.gtAny("2", 1L), is(false));
      fail("'gt' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String > "
              + "class java.lang.Long"));
    }
  }

  @Test void testGeWithAny() {
    // Non-numeric same type "greater or equal" check
    assertThat(SqlFunctions.geAny("banana", "apple"), is(true));
    assertThat(SqlFunctions.geAny("apple", "apple"), is(true));

    // Numeric types "greater or equal" check
    assertThat(SqlFunctions.geAny(2, 1L), is(true));
    assertThat(SqlFunctions.geAny(1, 1L), is(true));
    assertThat(SqlFunctions.geAny(2, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(1, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(2L, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(1L, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), 1), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), 1), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), 1L), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), 1L), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), 1.0D), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), 1.0D), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "greater or equal" check
    try {
      assertThat(SqlFunctions.geAny("2", 2L), is(false));
      fail("'ge' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String >= "
              + "class java.lang.Long"));
    }
  }

  @Test void testPlusAny() {
    // null parameters
    assertThat(SqlFunctions.plusAny(null, null), nullValue());
    assertThat(SqlFunctions.plusAny(null, 1), nullValue());
    assertThat(SqlFunctions.plusAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.plusAny(2, 1L), is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(2, 1.0D), is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(2L, 1.0D), is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), 1),
        is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), 1L),
        is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), 1.0D),
        is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is((Object) new BigDecimal(3)));

    // Non-numeric type
    try {
      SqlFunctions.plusAny("2", 2L);
      fail("'plus' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String + "
              + "class java.lang.Long"));
    }
  }

  @Test void testMinusAny() {
    // null parameters
    assertThat(SqlFunctions.minusAny(null, null), nullValue());
    assertThat(SqlFunctions.minusAny(null, 1), nullValue());
    assertThat(SqlFunctions.minusAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.minusAny(2, 1L), is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(2, 1.0D), is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(2L, 1.0D), is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), 1),
        is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), 1L),
        is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), 1.0D),
        is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is((Object) new BigDecimal(1)));

    // Non-numeric type
    try {
      SqlFunctions.minusAny("2", 2L);
      fail("'minus' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String - "
              + "class java.lang.Long"));
    }
  }

  @Test void testMultiplyAny() {
    // null parameters
    assertThat(SqlFunctions.multiplyAny(null, null), nullValue());
    assertThat(SqlFunctions.multiplyAny(null, 1), nullValue());
    assertThat(SqlFunctions.multiplyAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.multiplyAny(2, 1L), is(new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(2, 1.0D),
        is(new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(2L, 1.0D),
        is(new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), 1),
        is(new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), 1L),
        is(new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), 1.0D),
        is(new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(new BigDecimal(2)));

    // Non-numeric type
    try {
      SqlFunctions.multiplyAny("2", 2L);
      fail("'multiply' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String * "
              + "class java.lang.Long"));
    }
  }

  @Test void testDivideAny() {
    // null parameters
    assertThat(SqlFunctions.divideAny(null, null), nullValue());
    assertThat(SqlFunctions.divideAny(null, 1), nullValue());
    assertThat(SqlFunctions.divideAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.divideAny(5, 2L),
        is(new BigDecimal("2.5")));
    assertThat(SqlFunctions.divideAny(5, 2.0D),
        is(new BigDecimal("2.5")));
    assertThat(SqlFunctions.divideAny(5L, 2.0D),
        is(new BigDecimal("2.5")));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), 2),
        is(new BigDecimal(2.5)));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), 2L),
        is(new BigDecimal(2.5)));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), 2.0D),
        is(new BigDecimal(2.5)));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), new BigDecimal(2.0D)),
        is(new BigDecimal(2.5)));

    // Non-numeric type
    try {
      SqlFunctions.divideAny("5", 2L);
      fail("'divide' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String / "
              + "class java.lang.Long"));
    }
  }

  @Test void testMultiset() {
    final List<String> abacee = Arrays.asList("a", "b", "a", "c", "e", "e");
    final List<String> adaa = Arrays.asList("a", "d", "a", "a");
    final List<String> addc = Arrays.asList("a", "d", "c", "d", "c");
    final List<String> z = Collections.emptyList();
    assertThat(SqlFunctions.multisetExceptAll(abacee, addc),
        is(Arrays.asList("b", "a", "e", "e")));
    assertThat(SqlFunctions.multisetExceptAll(abacee, z), is(abacee));
    assertThat(SqlFunctions.multisetExceptAll(z, z), is(z));
    assertThat(SqlFunctions.multisetExceptAll(z, addc), is(z));

    assertThat(SqlFunctions.multisetExceptDistinct(abacee, addc),
        is(Arrays.asList("b", "e")));
    assertThat(SqlFunctions.multisetExceptDistinct(abacee, z),
        is(Arrays.asList("a", "b", "c", "e")));
    assertThat(SqlFunctions.multisetExceptDistinct(z, z), is(z));
    assertThat(SqlFunctions.multisetExceptDistinct(z, addc), is(z));

    assertThat(SqlFunctions.multisetIntersectAll(abacee, addc),
        is(Arrays.asList("a", "c")));
    assertThat(SqlFunctions.multisetIntersectAll(abacee, adaa),
        is(Arrays.asList("a", "a")));
    assertThat(SqlFunctions.multisetIntersectAll(adaa, abacee),
        is(Arrays.asList("a", "a")));
    assertThat(SqlFunctions.multisetIntersectAll(abacee, z), is(z));
    assertThat(SqlFunctions.multisetIntersectAll(z, z), is(z));
    assertThat(SqlFunctions.multisetIntersectAll(z, addc), is(z));

    assertThat(SqlFunctions.multisetIntersectDistinct(abacee, addc),
        is(Arrays.asList("a", "c")));
    assertThat(SqlFunctions.multisetIntersectDistinct(abacee, adaa),
        is(Collections.singletonList("a")));
    assertThat(SqlFunctions.multisetIntersectDistinct(adaa, abacee),
        is(Collections.singletonList("a")));
    assertThat(SqlFunctions.multisetIntersectDistinct(abacee, z), is(z));
    assertThat(SqlFunctions.multisetIntersectDistinct(z, z), is(z));
    assertThat(SqlFunctions.multisetIntersectDistinct(z, addc), is(z));

    assertThat(SqlFunctions.multisetUnionAll(abacee, addc),
        is(Arrays.asList("a", "b", "a", "c", "e", "e", "a", "d", "c", "d", "c")));
    assertThat(SqlFunctions.multisetUnionAll(abacee, z), is(abacee));
    assertThat(SqlFunctions.multisetUnionAll(z, z), is(z));
    assertThat(SqlFunctions.multisetUnionAll(z, addc), is(addc));

    assertThat(SqlFunctions.multisetUnionDistinct(abacee, addc),
        is(Arrays.asList("a", "b", "c", "d", "e")));
    assertThat(SqlFunctions.multisetUnionDistinct(abacee, z),
        is(Arrays.asList("a", "b", "c", "e")));
    assertThat(SqlFunctions.multisetUnionDistinct(z, z), is(z));
    assertThat(SqlFunctions.multisetUnionDistinct(z, addc),
        is(Arrays.asList("a", "c", "d")));
  }

  @Test void testMd5() {
    assertThat("d41d8cd98f00b204e9800998ecf8427e", is(md5("")));
    assertThat("d41d8cd98f00b204e9800998ecf8427e", is(md5(ByteString.of("", 16))));
    assertThat("902fbdd2b1df0c4f70b4a5d23525e932", is(md5("ABC")));
    assertThat("902fbdd2b1df0c4f70b4a5d23525e932",
        is(md5(new ByteString("ABC".getBytes(UTF_8)))));
    try {
      String o = md5((String) null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
  }

  @Test void testSha1() {
    assertThat("da39a3ee5e6b4b0d3255bfef95601890afd80709", is(sha1("")));
    assertThat("da39a3ee5e6b4b0d3255bfef95601890afd80709", is(sha1(ByteString.of("", 16))));
    assertThat("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8", is(sha1("ABC")));
    assertThat("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8",
        is(sha1(new ByteString("ABC".getBytes(UTF_8)))));
    try {
      String o = sha1((String) null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
  }

  @Test void testSha256() {
    assertThat("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        is(sha256("")));
    assertThat("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        is(sha256(ByteString.of("", 16))));
    assertThat("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e",
        is(sha256("Hello World")));
    assertThat("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e",
        is(sha256(new ByteString("Hello World".getBytes(UTF_8)))));
    try {
      String o = sha256((String) null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
  }

  @Test void testSha512() {
    assertThat("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5"
            + "d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
        is(sha512("")));
    assertThat("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5"
            + "d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
        is(sha512(ByteString.of("", 16))));
    assertThat("2c74fd17edafd80e8447b0d46741ee243b7eb74dd2149a0ab1b9246fb30382f27e853d858"
            + "5719e0e67cbda0daa8f51671064615d645ae27acb15bfb1447f459b",
        is(sha512("Hello World")));
    assertThat("2c74fd17edafd80e8447b0d46741ee243b7eb74dd2149a0ab1b9246fb30382f27e853d858"
            + "5719e0e67cbda0daa8f51671064615d645ae27acb15bfb1447f459b",
        is(sha512(new ByteString("Hello World".getBytes(UTF_8)))));
    try {
      String o = sha512((String) null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
  }

  /**
   * Tests that a date in the local time zone converts to a Unix timestamp in
   * UTC.
   */
  @Test void testToIntWithSqlDate() {
    assertThat(toInt(new java.sql.Date(0L)), is(0));  // rounded to closest day
    assertThat(sqlDate("1970-01-01"), is(0));
    assertThat(sqlDate("1500-04-30"), is(dateStringToUnixDate("1500-04-30")));
  }

  /**
   * Test calendar conversion from the standard Gregorian calendar used by
   * {@code java.sql} and the proleptic Gregorian calendar used by Unix
   * timestamps.
   */
  @Test void testToIntWithSqlDateInGregorianShift() {
    assertThat(sqlDate("1582-10-04"), is(dateStringToUnixDate("1582-10-04")));
    assertThat(sqlDate("1582-10-05"), is(dateStringToUnixDate("1582-10-15")));
    assertThat(sqlDate("1582-10-15"), is(dateStringToUnixDate("1582-10-15")));
  }

  /**
   * Test date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>Java may not be able to represent 0001-01-01 depending on the default
   * time zone. If the date would fall outside of Anno Domini (AD) when
   * converted to the default time zone, that date should not be tested.
   *
   * <p>Not every time zone has a January 1st 12:00am, so this test skips those
   * dates.
   */
  @Test void testToIntWithSqlDateInAnsiDateRange() {
    for (int i = 2; i <= 9999; ++i) {
      final String str = String.format(Locale.ROOT, "%04d-01-01", i);
      final java.sql.Date date = java.sql.Date.valueOf(str);
      final Timestamp timestamp = new Timestamp(date.getTime());
      if (timestamp.toString().endsWith("00:00:00.0")) {
        // Test equality if the time is valid in Java
        assertThat("Converts '" + str + "' from SQL to Unix date",
            toInt(date),
            is(dateStringToUnixDate(str)));
      } else {
        // Test result matches legacy behavior if the time cannot be
        // represented in Java. This probably results in a different date but
        // is pretty rare.
        final long expected =
            (date.getTime() + DateTimeUtils.DEFAULT_ZONE.getOffset(date.getTime()))
                / DateTimeUtils.MILLIS_PER_DAY;
        assertThat("Converts '" + str
                + "' from SQL to Unix date using legacy behavior",
            toInt(date),
            is((int) expected));
      }
    }
  }

  /**
   * Test using a custom {@link TimeZone} to calculate the Unix timestamp.
   * Dates created by a {@link java.sql.Date} method should be converted
   * relative to the local time and not UTC.
   */
  @Test public void testToIntWithTimeZone() {
    // Dates created by a Calendar should be converted to a Unix date in that
    // time zone
    final Calendar utcCal =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
    utcCal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
    utcCal.set(Calendar.MILLISECOND, 0);
    assertThat(
        toInt(new java.sql.Date(utcCal.getTimeInMillis()),
            utcCal.getTimeZone()),
        is(0));

    // Dates should be relative to the local time and not UTC
    final java.sql.Date epoch = java.sql.Date.valueOf("1970-01-01");

    final TimeZone minusDayZone = TimeZone.getDefault();
    minusDayZone.setRawOffset((int) (minusDayZone.getRawOffset() - MILLIS_PER_DAY));
    assertThat(toInt(epoch, minusDayZone), is(-1));

    final TimeZone plusDayZone = TimeZone.getDefault();
    plusDayZone.setRawOffset((int) (plusDayZone.getRawOffset() + MILLIS_PER_DAY));
    assertThat(toInt(epoch, plusDayZone), is(1));
  }

  /**
   * Tests that a nullable date in the local time zone converts to a Unix
   * timestamp in UTC.
   */
  @Test void testToIntOptionalWithLocalTimeZone() {
    assertThat(toIntOptional(java.sql.Date.valueOf("1970-01-01")), is(0));
    assertThat(toIntOptional((java.sql.Date) null), is(nullValue()));
  }

  /**
   * Tests that a nullable date in the given time zone converts to a Unix
   * timestamp in UTC.
   */
  @Test void testToIntOptionalWithCustomTimeZone() {
    final TimeZone utc = TimeZone.getTimeZone("UTC");
    assertThat(toIntOptional(new java.sql.Date(0L), utc), is(0));
    assertThat(toIntOptional(null, utc), is(nullValue()));
  }

  /**
   * Tests that a time in the local time zone converts to a Unix time in UTC.
   */
  @Test void testToIntWithSqlTime() {
    assertThat(sqlTime("00:00:00"), is(timeStringToUnixDate("00:00:00")));
    assertThat(sqlTime("23:59:59"), is(timeStringToUnixDate("23:59:59")));
  }

  /**
   * Tests that a nullable time in the local time zone converts to a Unix time
   * in UTC.
   */
  @Test void testToIntOptionalWithSqlTime() {
    assertThat(toIntOptional(Time.valueOf("00:00:00")), is(0));
    assertThat(toIntOptional((Time) null), is(nullValue()));
  }

  /**
   * Tests that a timestamp in the local time zone converts to a Unix timestamp
   * in UTC.
   */
  @Test void testToLongWithSqlTimestamp() {
    assertThat(sqlTimestamp("1970-01-01 00:00:00"), is(0L));
    assertThat(sqlTimestamp("2014-09-30 15:28:27.356"),
        is(timestampStringToUnixDate("2014-09-30 15:28:27.356")));
    assertThat(sqlTimestamp("1500-04-30 12:00:00.123"),
        is(timestampStringToUnixDate("1500-04-30 12:00:00.123")));
  }

  /**
   * Test using a custom {@link TimeZone} to calculate the Unix timestamp.
   * Timestamps created by a {@link Calendar} should be converted to a Unix
   * timestamp in the given time zone. Timestamps created by a {@link Timestamp}
   * method should be converted relative to the local time and not UTC.
   */
  @Test void testToLongWithSqlTimestampAndCustomTimeZone() {
    final Timestamp epoch = java.sql.Timestamp.valueOf("1970-01-01 00:00:00");

    final Calendar utcCal =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
    utcCal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
    utcCal.set(Calendar.MILLISECOND, 0);
    assertThat(toLong(new Timestamp(utcCal.getTimeInMillis()), utcCal.getTimeZone()),
        is(0L));

    final TimeZone est = TimeZone.getTimeZone("GMT-5:00");
    assertThat(toLong(epoch, est),
        is(epoch.getTime() + est.getOffset(epoch.getTime())));

    final TimeZone ist = TimeZone.getTimeZone("GMT+5:00");
    assertThat(toLong(epoch, ist),
        is(epoch.getTime() + ist.getOffset(epoch.getTime())));
  }

  /**
   * Test calendar conversion from the standard Gregorian calendar used by
   * {@code java.sql} and the proleptic Gregorian calendar used by Unix
   * timestamps.
   */
  @Test void testToLongWithSqlTimestampInGregorianShift() {
    assertThat(sqlTimestamp("1582-10-04 00:00:00"),
        is(timestampStringToUnixDate("1582-10-04 00:00:00")));
    assertThat(sqlTimestamp("1582-10-05 00:00:00"),
        is(timestampStringToUnixDate("1582-10-15 00:00:00")));
    assertThat(sqlTimestamp("1582-10-15 00:00:00"),
        is(timestampStringToUnixDate("1582-10-15 00:00:00")));
  }

  /**
   * Test date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>Java may not be able to represent 0001-01-01 depending on the default
   * time zone. If the date would fall outside of Anno Domini (AD) when
   * converted to the default time zone, that date should not be tested.
   *
   * <p>Not every time zone has a January 1st 12:00am, so this test skips those
   * dates.
   */
  @Test void testToLongWithSqlTimestampInAnsiDateRange() {
    for (int i = 2; i <= 9999; ++i) {
      final String str = String.format(Locale.ROOT, "%04d-01-01 00:00:00", i);
      final Timestamp timestamp = Timestamp.valueOf(str);
      if (timestamp.toString().endsWith("00:00:00.0")) {
        // Test equality if the time is valid in Java
        assertThat("Converts '" + str + "' from SQL to Unix timestamp",
            toLong(timestamp),
            is(timestampStringToUnixDate(str)));
      } else {
        // Test result matches legacy behavior if the time cannot be represented in Java
        // This probably results in a different date but is pretty rare
        final long expected = timestamp.getTime()
            + DateTimeUtils.DEFAULT_ZONE.getOffset(timestamp.getTime());
        assertThat("Converts '" + str
                + "' from SQL to Unix timestamp using legacy behavior",
            toLong(timestamp),
            is(expected));
      }
    }
  }

  /**
   * Tests that a nullable timestamp in the local time zone converts to a Unix
   * timestamp in UTC.
   */
  @Test void testToLongOptionalWithLocalTimeZone() {
    assertThat(toLongOptional(Timestamp.valueOf("1970-01-01 00:00:00")), is(0L));
    assertThat(toLongOptional(null), is(nullValue()));
  }

  /**
   * Tests that a nullable timestamp in the given time zone converts to a Unix
   * timestamp in UTC.
   */
  @Test void testToLongOptionalWithCustomTimeZone() {
    final TimeZone utc = TimeZone.getTimeZone("UTC");
    assertThat(toLongOptional(new Timestamp(0L), utc), is(0L));
    assertThat(toLongOptional(null, utc), is(nullValue()));
  }

  /**
   * Tests that a Unix timestamp converts to a date in the local time zone.
   */
  @Test void testInternalToDate() {
    assertThat(internalToDate(0), is(java.sql.Date.valueOf("1970-01-01")));
    assertThat(internalToDate(dateStringToUnixDate("1500-04-30")),
        is(java.sql.Date.valueOf("1500-04-30")));
  }

  /**
   * Test calendar conversion from the standard Gregorian calendar used by
   * {@code java.sql} and the proleptic Gregorian calendar used by Unix
   * timestamps.
   */
  @Test void testInternalToDateWithGregorianShift() {
    // Gregorian shift
    assertThat(internalToDate(dateStringToUnixDate("1582-10-04")),
        is(java.sql.Date.valueOf("1582-10-04")));
    assertThat(internalToDate(dateStringToUnixDate("1582-10-05")),
        is(java.sql.Date.valueOf("1582-10-15")));
    assertThat(internalToDate(dateStringToUnixDate("1582-10-15")),
        is(java.sql.Date.valueOf("1582-10-15")));
  }

  /**
   * Test date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>Java may not be able to represent all dates depending on the default time zone, but both
   * the expected and actual assertion values handles that in the same way.
   */
  @Test void testInternalToDateWithAnsiDateRange() {
    for (int i = 2; i <= 9999; ++i) {
      final String str = String.format(Locale.ROOT, "%04d-01-01", i);
      assertThat(internalToDate(dateStringToUnixDate(str)),
          is(java.sql.Date.valueOf(str)));
    }
  }

  /**
   * Tests that a Unix time converts to a SQL time in the local time zone.
   */
  @Test void testInternalToTime() {
    assertThat(internalToTime(0), is(Time.valueOf("00:00:00")));
    assertThat(internalToTime(86399000), is(Time.valueOf("23:59:59")));
  }

  /**
   * Tests that timestamp can be converted to a string given a custom pattern.
   */
  @Test void testToChar() {
    String pattern1 = "YYYY-MM-DD HH24:MI:SS.MS";
    String pattern2 = "Day, DD HH12:MI:SS";

    assertThat(
        toChar(0, pattern1),
        is("1970-01-01 00:00:00.000"));

    assertThat(
        toChar(0, pattern2),
        is("Thursday, 01 12:00:00"));

    assertThat(
        toChar(timestampStringToUnixDate("2014-09-30 15:28:27.356"), pattern1),
        is("2014-09-30 15:28:27.356"));

    assertThat(
        toChar(timestampStringToUnixDate("2014-09-30 15:28:27.356"), pattern2),
        is("Tuesday, 30 03:28:27"));

    assertThat(
        toChar(timestampStringToUnixDate("1500-04-30 12:00:00.123"), pattern1),
        is("1500-04-30 12:00:00.123"));
  }

  /**
   * Tests that a Unix timestamp converts to a SQL timestamp in the local time
   * zone.
   */
  @Test void testInternalToTimestamp() {
    assertThat(internalToTimestamp(0),
        is(Timestamp.valueOf("1970-01-01 00:00:00.0")));
    assertThat(internalToTimestamp(timestampStringToUnixDate("2014-09-30 15:28:27.356")),
        is(Timestamp.valueOf("2014-09-30 15:28:27.356")));
    assertThat(internalToTimestamp(timestampStringToUnixDate("1500-04-30 12:00:00.123")),
        is(Timestamp.valueOf("1500-04-30 12:00:00.123")));
  }

  /**
   * Test calendar conversion from the standard Gregorian calendar used by
   * {@code java.sql} and the proleptic Gregorian calendar used by Unix timestamps.
   */
  @Test void testInternalToTimestampWithGregorianShift() {
    assertThat(
        internalToTimestamp(timestampStringToUnixDate("1582-10-04 00:00:00")),
        is(Timestamp.valueOf("1582-10-04 00:00:00.0")));
    assertThat(
        internalToTimestamp(timestampStringToUnixDate("1582-10-05 00:00:00")),
        is(Timestamp.valueOf("1582-10-15 00:00:00.0")));
    assertThat(
        internalToTimestamp(timestampStringToUnixDate("1582-10-15 00:00:00")),
        is(Timestamp.valueOf("1582-10-15 00:00:00.0")));
  }

  /**
   * Test date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>Java may not be able to represent all dates depending on the default
   * time zone, but both the expected and actual assertion values handles that
   * in the same way.
   */
  @Test void testInternalToTimestampWithAnsiDateRange() {
    for (int i = 2; i <= 9999; ++i) {
      final String str = String.format(Locale.ROOT, "%04d-01-01 00:00:00", i);
      assertThat(internalToTimestamp(timestampStringToUnixDate(str)),
          is(Timestamp.valueOf(str)));
    }
  }

  private int sqlDate(String str) {
    return toInt(java.sql.Date.valueOf(str));
  }

  private int sqlTime(String str) {
    return toInt(java.sql.Time.valueOf(str));
  }

  private long sqlTimestamp(String str) {
    return toLong(java.sql.Timestamp.valueOf(str));
  }
}
