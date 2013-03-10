package org.cloudname.copkg.util;

import java.io.File;
import java.io.IOException;

/**
 * Utility for traversing files.  Use like this:
 *
 * <pre>
 *   new TraverseFiles() {
 *     @Override public void after(final File f) {
 *       // do stuff with f
 *     }
 *   }.traverse(new File("mydir"));
 * </pre>
 *
 * @author borud
 */
public class Traverse {
    public final void traverse(final File f) throws IOException {
        // If we have a directory we recurse
        if (f.isDirectory()) {
            final File[] children = f.listFiles();
            for(File child : children) {
                traverse(child);
            }
        }
        after(f);
    }
    public void after(final File dir) {}
}