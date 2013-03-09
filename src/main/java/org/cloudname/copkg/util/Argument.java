package org.cloudname.copkg.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a command line argument.
 *
 * @author borud
 */
public class Argument {
    private String option;
    private String value;
    private String prefix;

    /**
     * @param option prefixed by "-" or "--" or nothing.  Not allowed to be {@code null}
     * @param value the value of the argument. {@null} if the argument had no value.
     * @param prefix the prefix of the option, which can be "-", "--" or nothing (empty string).
     */
    public Argument(String option, String value, String prefix) {
        checkNotNull(option);
        checkNotNull(prefix);

        this.option = option;
        this.value = value;
        this.prefix = prefix;
    }

    public String getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * @return {@code true} if the argument is just a bare word.
     *   ie. not a command line option prefixed by a "-" or a "--".
     */
    public boolean isWord() {
        return "".equals(prefix);
    }

    @Override
    public String toString() {
        return "Argument: { prefix='" + prefix
            + "', option='" + option
            + "', value=" + ((value == null) ? "<null>" : "'" + value + "'") + " }";
    }
}
