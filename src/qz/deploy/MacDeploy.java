/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.ShellUtilities;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author Tres Finocchiaro
 */
public class MacDeploy extends DeployUtilities {

    private static final Logger log = LoggerFactory.getLogger(MacDeploy.class);

    @Override
    public boolean hasStartupShortcut() {
        return ShellUtilities.executeAppleScript(
                "tell application \"System Events\" to get the name "
                        + "of every login item where name is \"" + getShortcutName() + "\" or "
                        + "name is \"" + getJarName() + "\"",
                getShortcutName(), getJarName()
        );
    }

    @Override
    public boolean hasDesktopShortcut() {
        return isSymlink(System.getProperty("user.home") + "/Desktop/" + getShortcutName());
    }

    @Override
    public boolean createStartupShortcut() {
        return ShellUtilities.executeAppleScript(
                "tell application \"System Events\" to make login item "
                        + "at end with properties {path:\"" + getJarPath() + "\", "
                        + "hidden:true, name:\"" + getShortcutName() + "\"}"
        );
    }

    @Override
    public String getJarPath() {
        String jarPath = super.getJarPath();
        try {
            jarPath = URLDecoder.decode(jarPath, "UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            log.error("Error decoding URL: {}", jarPath, e);
        }

        return jarPath;
    }

    /**
     * Returns the jar's filename, which may be the identity of the startup
     * entry
     *
     * @return The jar's filename without the preceeding path information
     */
    private String getJarName() {
        return new File(getJarPath()).getName();
    }

    @Override
    public boolean createDesktopShortcut() {
        String target = getJarPath();
        if (target.contains("/Applications/")) {
            // Use the parent folder instead i.e. "/Applications/QZ Tray.app"
            File f = new File(getJarPath());
            if (f.getParent() != null) {
                target = f.getParent();
            }
        }

        return ShellUtilities.execute(new String[] {
                "ln", "-sf", target,
                System.getProperty("user.home") + "/Desktop/" + getShortcutName()
        });
    }

    @Override
    public boolean removeStartupShortcut() {
        return ShellUtilities.executeAppleScript(
                "tell application \"System Events\" to delete "
                        + "every login item where name is \"" + getShortcutName() + "\" or "
                        + "name is \"" + getJarName() + "\""
        );
    }

    @Override
    public boolean removeDesktopShortcut() {
        return ShellUtilities.execute(new String[] {
                "unlink",
                System.getProperty("user.home") + "/Desktop/" + getShortcutName()
        });
    }

    /**
     * Determines if the specified file is a symbolic link.
     *
     * @param filePath path of file to check for symbolic link
     * @return true if a symbolic link is found
     */
    public static boolean isSymlink(String filePath) {
        log.info("Verifying symbolic link: {}", filePath);
        boolean returnVal = false;
        if (filePath != null) {
            File f = new File(filePath);
            if (f.exists()) {
                try {
                    File canonicalFile = (f.getParent() == null? f:f.getParentFile().getCanonicalFile());
                    returnVal = !canonicalFile.getCanonicalFile().equals(canonicalFile.getAbsoluteFile());
                }
                catch(IOException ex) {
                    log.error("IOException checking for symlink", ex);
                }
            }
        }

        log.info("Symbolic link result: {}", returnVal);
        return returnVal;
    }

    /**
     * Executes a native AppleScript macro against the OS, useful for creating
     * shortcuts
     *
     * @param scriptBody
     * @param parseOutput
     * @param lookFor
     * @return
     *
     * private static boolean callAppleScript(String scriptBody, String[]
     * lookFor) { log.log(Level.INFO, "Running AppleScript: {0} {1} {2}", new
     * Object[]{"osascript", "-e", scriptBody});
     *
     * try { // Create and execute our new process Process p =
     * Runtime.getRuntime().exec(new String[]{"osascript", "-e", scriptBody});
     * if (lookFor != null) { return parseOutput(p, lookFor); } else {
     * p.waitFor(); return p.exitValue() == 0; } } catch (InterruptedException
     * ex) { log.log(Level.WARNING, "AppleScript process was interrupted while
     * executing: {0}{1}", new Object[]{scriptBody, ex.getLocalizedMessage()});
     * } catch (IOException ex) { log.log(Level.SEVERE, "AppleScript IO Error
     * while executing: {0}{1}", new Object[]{scriptBody,
     * ex.getLocalizedMessage()}); } return false; }
     *
     * /**
     *
     * @return
     *
     * private static boolean parseOutput(Process p, String[] lookFor) throws
     * IOException { BufferedReader stdInput = new BufferedReader(new
     * InputStreamReader(p.getInputStream())); String s; while ((s =
     * stdInput.readLine()) != null) { for (String search : lookFor) { if
     * (s.contains(search.trim())) { return true; } } } return false;
    }
     */
}
