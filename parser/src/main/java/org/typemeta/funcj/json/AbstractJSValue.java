package org.typemeta.funcj.json;

import org.typemeta.funcj.functions.Functions;

/**
 * Base class for classes that represent JSON values.
 */
public abstract class AbstractJSValue implements JSValue {

    public abstract StringBuilder toString(StringBuilder sb);

    public abstract <T> T match(
            Functions.F<JSNull, T> fNull,
            Functions.F<JSBool, T> fBool,
            Functions.F<JSNumber, T> fNum,
            Functions.F<JSString, T> fStr,
            Functions.F<JSArray, T> fArr,
            Functions.F<JSObject, T> fObj
    );

    public boolean isNull() {
        return false;
    }

    public boolean isBool() {
        return false;
    }

    public boolean isNumber() {
        return false;
    }

    public boolean isString() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isObject() {
        return false;
    }

    public JSNull asNull() {
        throw Utils.nullTypeError(getClass());
    }

    public JSBool asBool() {
        throw Utils.boolTypeError(getClass());
    }

    public JSNumber asNumber() {
        throw Utils.numberTypeError(getClass());
    }

    public JSString asString() {
        throw Utils.stringTypeError(getClass());
    }

    public JSArray asArray() {
        throw Utils.arrayTypeError(getClass());
    }

    public JSObject asObject() {
        throw Utils.objectTypeError(getClass());
    }
}
