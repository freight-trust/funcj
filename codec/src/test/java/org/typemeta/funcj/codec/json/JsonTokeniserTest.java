package org.typemeta.funcj.codec.json;

import org.junit.Test;
import org.typemeta.funcj.codec.json.io.*;

public class JsonTokeniserTest {
    @Test
    public void test() throws Exception {
        FileUtils.openResource("/example.json")
                .map(JsonTokeniser::new)
                .map(jt -> {
                    JsonIO.Input.Event ev;
                    while ((ev = jt.getNextEvent()) != JsonIO.Input.Event.Type.EOF) {
                        //System.out.println(ev);
                    }
                    return 0;
                }).getOrThrow();
    }
}
