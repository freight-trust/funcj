package org.javafp.json;

import org.javafp.parsec4j.*;
import org.javafp.util.Functions;

public class GrammarTokenised {

    private static <T> T match(
            Token token,
            Functions.F<Token.TokenImpl, T> tokenImpl,
            Functions.F<Token.Num, T> num,
            Functions.F<Token.Str, T> str) {
        return token.match(tokenImpl, num, str);
    }

    private static Result<Token, Double> num(Input<Token> in, int pos) {
        return match(
            in.at(pos),
            ti -> Result.failure(pos),
            n -> Result.success(n.value, pos+1),
            s -> Result.failure(pos)
        );
    }

    private static Result<Token, String> str(Input<Token> in, int pos) {
        return match(
            in.at(pos),
            ti -> Result.failure(pos),
            n -> Result.failure(pos),
            s -> Result.success(s.value, pos+1)
        );
    }

    static {
        final Ref<Token, Node> node = Ref.of();

        final Parser<Token, Node> nulN =
            Parser.value(Token.TokenImpl.NULL, Node.NullNode.NULL);
        final Parser<Token, Node> boolT =
            Parser.value(Token.TokenImpl.TRUE, Node.BoolNode.TRUE);
        final Parser<Token, Node> boolF =
            Parser.value(Token.TokenImpl.FALSE, Node.BoolNode.FALSE);
        final Parser<Token, Token> LEFT_SQR_BR =
            Parser.value(Token.TokenImpl.LEFT_SQR_BR);
        final Parser<Token, Token> RIGHT_SQR_BR =
            Parser.value(Token.TokenImpl.RIGHT_SQR_BR);
        final Parser<Token, Token> COMMA =
            Parser.value(Token.TokenImpl.COMMA);
        final Parser<Token, Token> COLON =
            Parser.value(Token.TokenImpl.COLON);
        final Parser<Token, Token> LEFT_BRACE =
            Parser.value(Token.TokenImpl.LEFT_BRACE);
        final Parser<Token, Token> RIGHT_BRACE =
            Parser.value(Token.TokenImpl.RIGHT_BRACE);

        final Parser<Token, Node> boolN = boolT.or(boolF);
//
//        final Parser<Token, Node> numN =
//            Parser.of(
//                (Ctx<Token> ctx, Integer pos) -> num(ctx, pos),
//                false
//            ).map(Node::number);
//
//        final Parser<Token, Node> strN =
//            Parser.of(
//                (Ctx<Token> ctx, Integer pos) -> str(ctx, pos),
//                false
//            ).map(Node::string);
//
//        final Parser<Token, Node> arrN =
//            between(
//                LEFT_SQR_BR,
//                RIGHT_SQR_BR,
//                sepBy(node, COMMA)
//            ).map(Node::array);
//
//        final Parser<Token, Tuple2<String, Node>> field =
//            Parser.of(GrammarTokenised::str, false)
//                .andL(COLON)
//                .and(node)
//                .map(F2.of(Tuple2::of));
//
//        final Parser<Token, Node> objN =
//            between(
//                LEFT_BRACE,
//                RIGHT_BRACE,
//                sepBy(field, COMMA)
//            ).map(Node::object);
//
//        node.set(
//            choice(nulN, boolN, numN, strN, arrN, objN)
//        );
    }
}
