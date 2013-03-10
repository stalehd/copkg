package org.cloudname.copkg.util;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Yet another command line argument parser.
 *
 * <p>
 * Note that when you feed this parser command line options, the only
 * permissible forms are:
 *
 * <pre>
 *  -option="value"
 *  --option="value"
 *  -option
 *  --option
 * </pre>
 *
 * It will <b>NOT</b> accept the following forms:
 *
 * <pre>
 *   -option value
 *   -option = value
 *   --option = value
 *   -option = "value"
 * </pre>
 *
 * In other words: you must use "=" and you must not have any spaces
 * on either side of the "=" sign.  If you want support for these
 * forms then feel free to implement them.
 *
 * <p>
 * <em>
 * There are a bunch of really good command line argument parsers out
 * there, but a cursory examination of half a dozen argument parsers
 * suggests that none of them seem to do what we need.  The one thing
 * we need is to parse arbitrary arguments that have not been
 * pre-defined and convert them to an internal form we can work with
 * programmatically.  We can probably mangle some library until it can
 * do this, but it is quicker to just write what we need because what
 * we need isn't really very fancy.
 * </em>
 *
 * @author borud
 */
public class ArgumentParser {
    private static Pattern argumentPattern = Pattern.compile("^(-{0,2})([a-zA-Z0-9][a-zA-Z0-9-]*)(=(.*))?");

    public static List<Argument> parse(String[] args) {
        List<Argument> arguments = new ArrayList<Argument>(args.length);

        for (String arg : args) {
            Matcher m = argumentPattern.matcher(arg);
            if (m.matches()) {
                String prefix = m.group(1);
                String option = m.group(2);
                String value = m.group(4);
                arguments.add(new Argument(option, value, prefix));
            } else {
                // Add as bare word with no prefix and no value
                arguments.add(new Argument(arg, null, ""));
            }
        }
        return arguments;
    }
}
