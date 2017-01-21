package org.javafp.data;

import org.javafp.data.Functions.*;

import java.util.*;
import java.util.function.*;

/**
 * Simple monadic wrapper for computations which result in either a successfully computed value
 * or an error.
 * {@code Try}, {@code Try.success}, and {@code Try.flatMap} satisfy the 3 monad laws:
 *      LI: success(a).flatMap(f) = f(a)
 *      RI: m.flatMap(success) = m
 *      AS: m.flatMap(f).flatMap(g) = m.flatMap(x -&gt; f(x).flatMap(g)
 * Try is effectively a discriminated union of Success (which wraps a value)
 * and Failure (which wraps an exception).
 */
public interface Try<T> {

    /**
     * Create a Success object.
     */
    static <T> Try<T> success(T value) {
        return new Success<T>(value);
    }

    /**
     * Create a Failure object.
     */
    static <T> Try<T> failure(Exception error) {
        return new Failure<T>(error);
    }

    /**
     * Create a Try value from a function which either yields a result or throws.
     */
    static <T> Try<T> of(FunctionsEx.F0<T> f) {
        try {
            return new Success<T>(f.apply());
        } catch (Exception ex) {
            return new Failure<T>(ex);
        }
    }

    /**
     * Standard monadic sequencing.
     */
    static <T> Try<IList<T>> sequence(IList<Try<T>> lt) {
        return lt.foldr(
                (tt, tlt) -> tt.flatMap(t -> tlt.flatMap(l -> success((IList<T>)l.add(t)))),
                Try.success(IList.nil())
        );
    }

    /**
     * Do we have a success result?
     */
    boolean isSuccess();

    /**
     * Either return the result value, otherwise return the supplied default value.
     */
    T getOrElse(T defaultValue);

    /**
     * Either return the result value, otherwise throw the result exception.
     */
    T getOrThrow() throws Exception;

    /**
     * Similar to getOrThrow but will throw a RuntimeException.
     */
    T get();

    /**
     * Push the result to a Consumer.
     */
    void handle(Consumer<T> success, Consumer<Exception> failure);

    /**
     * Map either function over the result.
     */
    <R> R match(F<Success<T>, ? extends R> success, F<Failure<T>, ? extends R> failure);

    /**
     * Functor function application.
     * Apply the function to the value held within this result.
     */
    <R> Try<R> map(F<? super T, ? extends R> f);

    /**
     * If this is a success then apply the function to the value and return the result,
     * Otherwise return the failure result.
     */
    <R> Try<R> flatMap(F<? super T, Try<R>> f);

    /**
     * Variant of flatMap which ignores the supplied value.
     */
    default <R> Try<R> flatMap(F0<Try<R>> f) {
        return flatMap(unused -> f.apply());
    }

    /**
     * Successful result type, which wraps the result value.
     */
    final class Success<T> implements Try<T> {

        public final T value;

        private Success(T value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return value;
        }

        @Override
        public T getOrThrow() throws Exception {
            return value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public void handle(Consumer<T> success, Consumer<Exception> failure) {
            success.accept(value);
        }

        @Override
        public <R> R match(F<Success<T>, ? extends R> success, F<Failure<T>, ? extends R> failure) {
            return success.apply(this);
        }

        @Override
        public <R> Try<R> map(F<? super T, ? extends R> f) {
            return new Success<R>(f.apply(value));
        }

        @Override
        public <R> Try<R> flatMap(F<? super T, Try<R>> f) {
            return f.apply(value);
        }

        @Override
        public String toString() {
            return "Success(" + value + ")";
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null)
                return false;

            if (!(obj instanceof Success<?>))
                return false;

            final Success<?> rhs = (Success<?>)obj;

            return value.equals(rhs.value);
        }
    }

    /**
     * Unsuccessful result type. Wraps the result exception.
     */
    final class Failure<T> implements Try<T> {

        public final Exception error;

        private Failure(Exception error) {
            this.error = Objects.requireNonNull(error);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T getOrThrow() throws Exception {
            throw error;
        }

        @Override
        public T get() {
            throw new RuntimeException(error);
        }

        @Override
        public void handle(Consumer<T> success, Consumer<Exception> failure) {
            failure.accept(error);
        }

        @Override
        public <R> R match(F<Success<T>, ? extends R> success, F<Failure<T>, ? extends R> failure) {
            return failure.apply(this);
        }

        @Override
        public <R> Try<R> map(F<? super T, ? extends R> f) {
            return Try.failure(error);
        }

        @Override
        public <R> Try<R> flatMap(F<? super T, Try<R>> f) {
            return Try.failure(error);
        }

        @Override
        public String toString() {
            return "Failure(" + error + ")";
        }

        @Override
        public int hashCode() {
            return error.hashCode();
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null)
                return false;

            if (!(obj instanceof Failure<?>))
                return false;

            final Failure<?> rhs = (Failure<?>)obj;

            // In general the equals() method for Exception classes isn't implemented,
            // which means we get object equality. This is rarely useful so here
            // we instead compare the string representations.
            return error.toString().equals(rhs.error.toString());
        }
    }
}
