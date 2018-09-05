package org.typemeta.funcj.codec.jsons;

import org.typemeta.funcj.codec.CodecException;

import java.io.*;
import java.util.Arrays;
import java.util.function.Supplier;

public class JsonTokeniser {
    public interface Event {
        enum Enum implements Event {
            EOF,
            NULL,
            TRUE,
            FALSE,
            ARRAY_START,
            ARRAY_EMD,
            OBJECT_START,
            OBJECT_END,
            COMMA,
            COLON
        }

        class JString implements Event {
            public final String value;

            public JString(String value) {
                this.value = value;
            }
        }

        class JNumber implements Event {
            public final String value;

            public JNumber(String value) {
                this.value = value;
            }
        }
    }

    private static final class Buffer {
        private static final int DEFAULT_SIZE = 64;

        private char[] buffer;
        private int size = 0;

        Buffer() {
            buffer = new char[DEFAULT_SIZE];
        }

        void add(char c) {
            if (size == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }

            buffer[size++] = c;
        }

        boolean isEmpty() {
            return size == 0;
        }

        String release() {
            final String res = new String(buffer, 0, size);
            size = 0;
            return res;
        }

        @Override
        public String toString() {
            return new String(buffer, 0, size);
        }
    }

    private static final char[] TRUE = "true".toCharArray();
    private static final char[] FALSE = "false".toCharArray();
    private static final char[] NULL = "null".toCharArray();

    private Reader rdr;
    private long pos = 0;
    private final Buffer buffer;

    public JsonTokeniser(Reader rdr) {
        this.rdr = rdr;
        this.buffer = new Buffer();
    }

    private void raiseError(Supplier<String> msg) {
        throw new CodecException(msg.get() + " at position " + pos);
    }

    private int nextChar() throws IOException {
        int ic = rdr.read();
        ++pos;
        return ic;
    }

    private char nextCharOrThrow(Supplier<String> msg) throws IOException {
        int ic = rdr.read();
        if (ic == -1) {
            raiseError(msg);
        }
        ++pos;
        return (char)ic;
    }

    private char nextCharOrThrow() throws IOException {
        int ic = rdr.read();
        if (ic == -1) {
            raiseError(() -> "Unexpected end-of-input");
        }
        ++pos;
        return (char)ic;
    }

    private void parseSymbol(char[] s) throws IOException {
        for (int i = 1; i < s.length; ++i) {
            final char c = nextCharOrThrow();
            if (c != s[i]) {
                raiseError(() -> ("Unexpected input '" + c + "' while parsing '" + s + "'"));
            }
        }
    }

    enum NumState {
        A, B, C, D, E, F, G, H, I, Z
    }

    public Event getNextEvent() {
        if (rdr == null) {
            return Event.Enum.EOF;
        }

        try {
            int ic = nextChar();
            char nc = (char)ic;
            while (ic != -1 && Character.isWhitespace(nc)) {
                ic = rdr.read();
                nc = (char)ic;
            }

            if (ic == -1) {
                rdr = null;
                return Event.Enum.EOF;
            } else {
                final char c = nc;
                switch (c) {
                    case '{': return Event.Enum.OBJECT_START;
                    case '}': return Event.Enum.OBJECT_END;
                    case '[': return Event.Enum.ARRAY_START;
                    case ']': return Event.Enum.ARRAY_EMD;
                    case ',': return Event.Enum.COMMA;
                    case ':': return Event.Enum.COLON;
                    case '"': {
                        while (true) {
                            char c2 = nextCharOrThrow(() -> "Unexpected end-of-input while parsing a string");
                            if (c2 == '"') {
                                return new Event.JString(buffer.release());
                            }
                            buffer.add(c2);
                        }
                    }
                    case 't': {
                        parseSymbol(TRUE);
                        return Event.Enum.TRUE;
                    }
                    case 'f': {
                        parseSymbol(FALSE);
                        return Event.Enum.FALSE;
                    }
                    case 'n': {
                        parseSymbol(NULL);
                        return Event.Enum.NULL;
                    }
                    case '0':
                        return parseNumber(NumState.B);
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        return parseNumber(NumState.C);
                    case '-':
                    case '+':
                        return parseNumber(NumState.A);
                    default:
                        raiseError(() -> "Unexpected input '" + c + "' while parsing a number");
                }
            }
        } catch (IOException ex) {
            throw new CodecException(ex);
        }

        throw new CodecException("Illegal state (internal error)");
    }

    private Event.JNumber parseNumber(NumState state) throws IOException {
        int ic = nextChar();
        while (ic != -1 && state != NumState.Z) {
            char c = (char)ic;
            switch (state) {
                case A:
                    switch (c) {
                        case '0': {
                            state = NumState.B;
                            break;
                        }
                        case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9': {
                            state = NumState.C;
                            break;
                        }
                        default:
                            raiseError(() -> "Unexpected input '" + c + "' while parsing a number");
                    }
                    break;
                case B:
                    switch (c) {
                        case '.': {
                            state = NumState.D;
                            break;
                        }
                        case 'e':
                        case 'E': {
                            state = NumState.F;
                            break;
                        }
                        default: {
                            state = NumState.Z;
                            break;
                        }
                    }
                    break;
                case C:
                    switch (c) {
                        case '.': {
                            state = NumState.D;
                            break;
                        }
                        case 'e':
                        case 'E': {
                            state = NumState.F;
                            break;
                        }
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9': {
                            break;
                        }
                        default: {
                            state = NumState.Z;
                            break;
                        }
                    }
                    break;
                case D:
                    switch (c) {
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9': {
                            state = NumState.E;
                            break;
                        }
                        default:
                            raiseError(() -> "Unexpected input '" + c + "' while parsing a number");
                    }
                    break;
                case E:
                    switch (c) {
                        case 'e':
                        case 'E': {
                            state = NumState.H;
                            break;
                        }
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9':
                            break;
                        default:
                            raiseError(() -> "Unexpected input '" + c + "' while parsing a number");
                    }
                    break;
                case F:
                    switch (c) {
                        case '+':
                        case '-': {
                            state = NumState.G;
                            break;
                        }
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9': {
                            state = NumState.I;
                            break;
                        }
                        default:
                            raiseError(() -> "Unexpected input '" + c + "' while parsing a number");
                    }
                    break;
                case G:
                    switch (c) {
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9': {
                            state = NumState.I;
                            break;
                        }
                        default:
                            raiseError(() -> "Unexpected input '" + c + "' while parsing a number");
                    }
                    break;
                case H:
                    switch (c) {
                        case 'e':
                        case 'E': {
                            state = NumState.F;
                            break;
                        }
                        default: {
                            state = NumState.Z;
                            break;
                        }
                    }
                case I:
                    switch (c) {
                        case 'e':
                        case 'E':
                            break;
                        default: {
                            state = NumState.Z;
                            break;
                        }
                    }

                case Z:
                    break;
            }
            if (state != NumState.Z) {
                buffer.add(c);
            }
        }

        switch (state) {
            case A:
            case D:
            case F:
            case G:
                raiseError(() -> "Unexpected end-of-input while parsing a number");
            default:
                return new Event.JNumber(buffer.release());
        }
    }

}
