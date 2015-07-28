package fr.cea.ig.grools.server.common;
/*
 * Copyright LABGeM 24/02/15
 *
 * author: Jonathan MERCIER
 *
 * This software is a computer program whose purpose is to annotate a complete genome.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
/*
 * @startuml
 * class Command{
 *  {static} +run( final String cmd, final List<String> params): List<BufferedReader>
 *  {static} +run( final List<String> command ): List<BufferedReader>
 * }
 * @enduml
 */
public class Command {
    public static List<BufferedReader> run( final String cmd, final List<String> params) throws Exception {

        List<String> command = new ArrayList<String>(Arrays.asList(cmd));
        command.addAll(params);
        return run( command );
    }

    /**
     *
     * @param command run a command from a list usually space are used as splitter
     * @return output buffered at index 0 and error buffered at index 1
     * @throws IOException if error while trying to command output
     */
    public static List<BufferedReader> run( final List<String> command ) throws Exception {
        final Process             process = new ProcessBuilder( command ).start();
        final InputStream         out     = process.getInputStream();
        final InputStreamReader   outr    = new InputStreamReader(out, "UTF-8");
        final BufferedReader      outb    = new BufferedReader(outr);
        final InputStream         err     = process.getErrorStream();
        final InputStreamReader   errr    = new InputStreamReader(err, "UTF-8");
        final BufferedReader      errbr   = new BufferedReader(errr);
        synchronized(process) {
            try {
                process.waitFor();
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int exitStatus = process.exitValue();
        if( exitStatus != 0) {
            final StringBuilder msg = new StringBuilder();
            errbr.lines().forEach( line ->{
                msg.append( line );
            });
            throw new Exception(msg.toString());
        }
        return Arrays.asList(outb,errbr);
    }
}
