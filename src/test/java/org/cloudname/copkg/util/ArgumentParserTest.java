package org.cloudname.copkg.util;

import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit test for the ArgumentParser class.
 *
 * @author borud
 */
public class ArgumentParserTest {
    // A sample command line argument list
    private static final String[] args = {
        "foo",
        "-without-args",
        "-with-args=args string",
        "--with-double-dash",
        "--with-double-dash-and-args=the args"
    };

    /**
     * Parse the argument array, which emulates how arguments are fed
     * into the main method, and ensure that the result is what we
     * expect.
     */
    @Test
    public void testSimple() throws Exception {
        List<Argument> arguments = ArgumentParser.parse(args);
        assertNotNull(arguments);

        Argument arg0 = arguments.remove(0);
        assertEquals("", arg0.getPrefix());
        assertEquals("foo", arg0.getOption());
        assertNull(arg0.getValue());

        Argument arg1 = arguments.remove(0);
        assertEquals("-", arg1.getPrefix());
        assertEquals("without-args", arg1.getOption());
        assertNull(arg0.getValue());

        Argument arg2 = arguments.remove(0);
        assertEquals("-", arg2.getPrefix());
        assertEquals("with-args", arg2.getOption());
        assertEquals("args string", arg2.getValue());

        Argument arg3 = arguments.remove(0);
        assertEquals("--", arg3.getPrefix());
        assertEquals("with-double-dash", arg3.getOption());
        assertNull(arg3.getValue());

        Argument arg4 = arguments.remove(0);
        assertEquals("--", arg4.getPrefix());
        assertEquals("with-double-dash-and-args", arg4.getOption());
        assertEquals("the args", arg4.getValue());

        // Make sure that the arguments list is now empty
        assertEquals(0, arguments.size());
    }
}
