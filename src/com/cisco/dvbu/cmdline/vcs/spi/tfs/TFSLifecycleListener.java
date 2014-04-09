/*******************************************************************************
* Copyright (c) 2014 Cisco Systems
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* PDTool project commiters - initial release
*******************************************************************************/
/**
 * (c) 2014 Cisco and/or its affiliates. All rights reserved.
 */
package com.cisco.dvbu.cmdline.vcs.spi.tfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.cisco.dvbu.cmdline.vcs.spi.AbstractLifecycleListener;

/**
 * A TFS lifecycle listener.
 * 
 * NOTE: Warning handling may be specific to TFS.
 *
 * @author panagiotis,cgoodrich,mtinius
 */
public class TFSLifecycleListener extends AbstractLifecycleListener {

  // Instead of hard coding command line switches here (where it's hard to maintain), call TFSLL.bat in 
  // <cis_install_dir>\conf\studio\vcsScriptsFolder\generalized_scripts_studio (CIS_VCS_HOME gets set to
  // this by the checkin/checkout batch files before this class is used.) Edit the TFSLL.bat file to 
  // alter the command line options for each TFS command.
  //
  //private static final String TFS = System.getenv ("CIS_VCS_HOME") + "\\TFSLL.bat"; // CIS_VCS_HOME set by checkin/checkout batch scripts
  private static final String TFS = VCS_EXEC;

  private static final String[] TFS_ADD = new String[] { TFS, "add", ""};
  private static final String[] TFS_DELETE = new String[] { TFS, "delete", ""};
  private static final String[] TFS_COMMIT = new String[] { TFS, "checkin", ""};

  public TFSLifecycleListener() {}

// there's no abstract handle() method in the abstract class we're extending that needs to be overridden.
//@Override
  public void handle (
    File file,
    Event event,
    Mode mode,
    boolean verbose
  ) throws VCSException {

    switch (event) {

      case CREATE:
        switch (mode) {
          case POST:
            handle (file, TFS_ADD, 2, verbose);
            //handle (file, TFS_COMMIT, 2, verbose);
            break;

          default: // do nothing
        }
        break;

      case DELETE:
        switch (mode) {
          case PRE:
            handle (file, TFS_DELETE, 2, verbose);
            //handle (file, TFS_COMMIT, 2, verbose);
            break;

          default: // do nothing
        }
        break;

      default: // do nothing
    }
  }

  public void checkinPreambleFolder (
    File file,
    boolean verbose
  ) throws VCSException {
    handle (file, TFS_COMMIT, 2, verbose);
  }

  protected void handle (
    File file,
    String[] commandTemplate,
    int index,
    boolean verbose
  ) throws VCSException {

    // customize TFS command
    //
    Map<Integer, String> commandConfiguration = new HashMap<Integer, String>();

//    commandConfiguration.put (index, file.getName());
    commandConfiguration.put(index, file.getAbsolutePath());

    String[] command = getConfiguredCommand (commandTemplate, commandConfiguration);

    // execute TFS command
    //
    execute (file.getParentFile(), command, verbose);
  }

  protected String getErrorMessages (Process process) throws VCSException {
    StringBuilder result = new StringBuilder();
    BufferedReader sr = new BufferedReader (new InputStreamReader (process.getErrorStream()));

    try {
      String line = null;

      while ((line = sr.readLine()) != null) {
        
        // determine which lines from stderr to ignore
        //
        if (!line.contains("TFS: warning") &&
            !line.contains("TF10139:") &&           // this line and the next give a warning about associating a checked in resource with a "work item".
            !line.contains("You must associate") &&
            !line.contains("already exists")        // this ignores errors from TFS where an attempt to check in a resource results in an "already exists" error.
            )
          result.append (line).append(LS); // append a the line from stderr and a line separator
      }

    } catch (IOException e) {
      throw new VCSException(e);
    } finally {
      try { sr.close(); } catch (IOException e) {throw new VCSException (e);}
    }

    return result.toString(); // if result is anything other than a zero length StringBuffer, an exception will be thrown.
  }
}




