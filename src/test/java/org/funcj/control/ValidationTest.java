package org.funcj.control;

import org.junit.*;

public class ValidationTest {
    private static Validation<String, Double> parseDbl(String s) {
        return Validation.of(
            () -> Double.parseDouble(s),
            Exception::getMessage
        );
    }

    private static Validation<String, Integer> parseInt(String s) {
        return Validation.of(
            () -> Integer.parseInt(s),
            Exception::getMessage
        );
    }

    @Test
    public void testGoodGood() {
        final Validation<String, Double> r =
            parseInt("1234")
                .and(parseDbl("1.2"))
                .map(i -> d -> i+d);
        Assert.assertTrue("Validation is success", r.isSuccess());
    }

    @Test
    public void testGoodBad() {
        final Validation<String, Double> r =
            parseInt("1234")
                .and(parseDbl("1.2z"))
                .map(i -> d -> i+d);
        Assert.assertFalse("Validation is failure", r.isSuccess());
    }

    @Test
    public void testBadGood() {
        final Validation<String, Double> r =
            parseInt("1234z")
                .and(parseDbl("1.2"))
                .map(i -> d -> i+d);
        Assert.assertFalse("Validation is failure", r.isSuccess());
    }

    @Test
    public void testBadBad() {
        final Validation<String, Double> r =
            parseInt("1234z")
                .and(parseDbl("1.2z"))
                .map(i -> d -> i+d);
        Assert.assertFalse("Validation is failure", r.isSuccess());
    }
}