package org.funcj.parser;

import org.funcj.data.*;
import org.funcj.util.Functions.*;

import static org.funcj.parser.Parser.pure;
import static org.funcj.parser.Utils.*;
import static org.funcj.util.Functions.F2.curry;

/**
 * A parser is essentially a function from an input stream to a parse {@link org.funcj.parser.Result}.
 * The {@code Parser} type along with the {@code pure} and {@code ap} functions constitute an applicative functor.
 * @param <I> input stream symbol type
 * @param <A> parser result type
 */
public interface Parser<I, A> {

    /**
     * Indicate whether this parser accepts the empty symbol.
     * @return a lazy wrapper for true iff the parser accepts the empty symbol
     */
    Lazy<Boolean> acceptsEmpty();

    /**
     * The First Set for this parser.
     * @return lazy symbol set
     */
    Lazy<SymSet<I>> firstSet();

    /**
     * Apply this parser to the input stream.
     * @param in input stream
     * @param follow dynamic follow set
     * @return the parse result
     */
    Result<I, A> parse(Input<I> in, SymSet<I> follow);

    /**
     * Apply this parser to the input stream.
     * @param in input stream
     * @return the parser result
     */
    default Result<I, A> parse(Input<I> in) {
        return this.parse(in, SymSet.empty());
    }

    /**
     * Apply this parser to the input stream. Fail if eof isn't reached.
     * @param in input stream
     * @return the parser result
     */
    default Result<I, A> run(Input<I> in) {
        final Parser<I, A> parserAndEof = this.andL(Combinators.eof());
        if (acceptsEmpty().apply()) {
            return parserAndEof.parse(in, SymSet.empty());
        } else if (in.isEof()) {
            return failureEof(this, in);
        } else if (firstSet().apply().matches(in.get())) {
            return parserAndEof.parse(in, SymSet.empty());
        } else {
            return failure(this, in);
        }
    }

    /**
     * Applicative unit/pure function.
     * Construct a parser that always returns the supplied value, without consuming any input.
     * @param a value
     * @param <I> input stream symbol type
     * @param <A> parser result type
     * @return a parser that always returns the supplied value
     */
    static <I, A> Parser<I, A> pure(A a) {
        return new ParserImpl<I, A>(LTRUE, SymSet::empty) {
            @Override
            public Result<I, A> parse(Input<I> in, SymSet<I> follow) {
                return Result.success(a, in);
            }
        };
    }

    /**
     * Functor map operation.
     * If this parser succeeds then apply the function {@code f} to the result,
     * otherwise return the failure.
     * @param f function to be mapped over this parser
     * @param <B> function return type
     * @return a parser that returns {@code f} mapped over this parser's result
     */
    default <B> Parser<I, B> map(F<A, B> f) {
        return new ParserImpl<I, B>(
            Parser.this.acceptsEmpty(),
            Parser.this.firstSet()
        ) {
            @Override
            public Result<I, B> parse(Input<I> in, SymSet<I> follow) {
                return Parser.this.parse(in, follow).map(f);
            }
        };
    }

    /**
     * Applicative application.
     * Construct a parser that, if {@code pf} succeeds, yielding a function {@code f},
     * and if {@code pa} succeeds, yielding a value {@code a},
     * then it returns the result of applying function {@code f} to value {@code a}.
     * Otherwise, if {@code pf} fails then the parser returns the failure,
     * else if {@code pa} fails then it returns that failure.
     * @param pf parser that returns a function result
     * @param pa parser that returns a value result
     * @param <I> input stream symbol type
     * @param <A> input type of the function
     * @param <B> return type of the function
     * @return a parser that returns the result of applying the parsed function to the parsed value
     */
    static <I, A, B>
    Parser<I, B> ap(Parser<I, F<A, B>> pf, Parser<I, A> pa) {
        return new ParserImpl<I, B>(
            Utils.and(pf.acceptsEmpty(), pa.acceptsEmpty()),
            combine(pf.acceptsEmpty(), pf.firstSet(), pa.firstSet())
        ) {
            @Override
            public Result<I, B> parse(Input<I> in, SymSet<I> follow) {
                final SymSet<I> followF =
                    combine(
                        pa.acceptsEmpty().apply(),
                        pa.firstSet().apply(),
                        follow);

                final Result<I, F<A, B>> r = pf.parse(in, followF);

                if (r.isSuccess()) {
                    final Result.Success<I, F<A, B>> succ = (Result.Success<I, F<A, B>>) r;
                    final Result<I, A> r2 = pa.parse(succ.next(), follow);
                    return r2.map(succ.value());
                } else {
                    return ((Result.Failure<I, F<A, B>>) r).cast();
                }
            }
        };
    }

    /**
     * Applicative application.
     * Construct a parser that, if {@code pa} succeeds, yielding a function {@code a},
     * then it returns the result of applying function {@code f} to value {@code a}.
     * If {@code pa} fails then the parser returns the failure.
     * @param f a function
     * @param pa parser that returns a value result
     * @param <I> input stream symbol type
     * @param <A> input type of the function
     * @param <B> return type of the function
     * @return a parser that returns the result of applying the function to the parsed value
     */
    static <I, A, B>
    Parser<I, B> ap(F<A, B> f, Parser<I, A> pa) {
        return ap(pure(f), pa);
    }

    /**
     * Alternative.
     * Construct a parser which returns the result of either this parser or,
     * if it fails, then the result of the {@code rhs} parser.
     * @param rhs second parser to attempt
     * @return a parser which returns the result of either this parser or the {@code rhs} parser.
     */
    default Parser<I, A> or(Parser<I, A> rhs) {
        return new ParserImpl<I, A>(
            Utils.or(Parser.this.acceptsEmpty(), rhs.acceptsEmpty()),
            union(Parser.this.firstSet(), rhs.firstSet())
        ) {
            @Override
            public Result<I, A> parse(Input<I> in, SymSet<I> follow) {
                if (in.isEof()) {
                    if (Parser.this.acceptsEmpty().apply()) {
                        return Parser.this.parse(in, follow);
                    } else if (rhs.acceptsEmpty().apply()) {
                        return rhs.parse(in, follow);
                    } else {
                        return failureEof(this, in);
                    }
                } else {
                    final I next = in.get();
                    if (Parser.this.firstSet().apply().matches(next)) {
                        return Parser.this.parse(in, follow);
                    } else if (rhs.firstSet().apply().matches(next)) {
                        return rhs.parse(in, follow);
                    } else if (follow.matches(next)) {
                        if (Parser.this.acceptsEmpty().apply()) {
                            return Parser.this.parse(in, follow);
                        } else if (rhs.acceptsEmpty().apply()) {
                            return rhs.parse(in, follow);
                        }
                    }
                    return failure(this, in);
                }
            }
        };
    }

    /**
     * Combine this parser with another to form a parser which applies the two parsers,
     * and if they are both successful then returns the pair of results.
     * @param pb second parser
     * @param <B> result type of second parser
     * @return a parser that applies two parsers consecutively and returns the pair of values
     */
    default <B> Parser<I, Tuple2<A, B>> product(Parser<I, B> pb) {
        return ap(this.map(curry(Tuple2::new)), pb);
    }

    /**
     * Combine this parser with another to form a parser which applies two parsers,
     * and if they are both successful
     * throws away the result of the right-hand parser,
     * and returns the result of the left-hand parser
     * @param pb second parser
     * @param <B> result type of second parser
     * @return a parser that applies two parsers consecutively and returns the result of the first
     */
    default <B> Parser<I, A> andL(Parser<I, B> pb) {
        return this.and(pb).map(F2.first());
    }

    /**
     * Combine this parser with another to form a parser which applies two parsers,
     * and if they are both successful
     * throws away the result of the left-hand parser
     * and returns the result of the right-hand parser
     * @param pb second parser
     * @param <B> result type of second parser
     * @return a parser that applies two parsers consecutively and returns the result of the second
     */
    default <B> Parser<I, B> andR(Parser<I, B> pb) {
        return this.and(pb).map(F2.second());
    }

    /**
     * Combine this parser with another to form a builder which accumulates the parse results.
     * @param pb second parser
     * @param <B> result type of second parser
     * @return an {@link ApplyBuilder} which accumulates the parse results.
     */
    default <B> ApplyBuilder._2<I, A, B> and(Parser<I, B> pb) {
        return new ApplyBuilder._2<I, A, B>(this, pb);
    }

    /**
     * A parser for an operand, followed by one or more operands
     * that separated by operators.
     * This can, for example, be used to eliminate left recursion
     * which typically occurs in expression grammars.
     * @param op parser for the operator
     * @return a parser for operator expressions
     */
    default Parser<I, A> chainl1(Parser<I, Op2<A>> op) {
        final Parser<I, IList<Op<A>>> plOps =
                Combinators.many(op.and(this)
                        .map((f, y) -> x -> f.apply(x, y)));
        return this.and(plOps)
                .map((a, lf) -> lf.foldLeft((acc, f) -> f.apply(acc), a));
    }
}

/**
 * Base class for {@code Parser} implementations.
 * @param <I> input stream symbol type
 * @param <A> parser result type
 */
abstract class ParserImpl<I, A> implements Parser<I, A> {

    private final Lazy<Boolean> acceptsEmpty;

    private final Lazy<SymSet<I>> firstSet;

    ParserImpl(Lazy<Boolean> acceptsEmpty, Lazy<SymSet<I>> firstSet) {
        this.acceptsEmpty = acceptsEmpty;
        this.firstSet = firstSet;
    }

    public Lazy<Boolean> acceptsEmpty() {
        return acceptsEmpty;
    }

    public Lazy<SymSet<I>> firstSet() {
        return firstSet;
    }

    @Override
    public String toString() {
        return "parser{" +
            "empty=" + acceptsEmpty.apply() +
            ";first=" + firstSet.apply() +
            '}';
    }
}
