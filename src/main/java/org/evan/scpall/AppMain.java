package org.evan.scpall;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class AppMain {

    protected static Set<String> readClusterFile(String alias) {

        String fullFilePath = "/etc/clusters";
        Set<String> remoteSystems = new HashSet <>();

        String token = alias + " ";
        try {

            try (BufferedReader br = new BufferedReader(new FileReader(fullFilePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(token)) {
                        //We found the line. Parse out the system names.
                        remoteSystems = Pattern.compile("\\s+")
                                .splitAsStream(line)
                                .filter(l -> !l.equals(alias))
                            //    .map(l -> StringUtils.substringAfter(l, "@"))
                                .filter(StringUtils::isNotEmpty)
                                .sorted()
                                .collect(Collectors.toSet());

                        System.out.println("Writing to systems " + String.join(" ", remoteSystems));
                        return remoteSystems;
                    }

                }
            }
        } catch (Exception e) {
            System.out.println( "Unable to read hosts file - " + e.getMessage() );
        }

        System.out.println("Alias not found in /etc/clusters");
        exit(5);
        return remoteSystems;
    }


    public static void scpTheFileToAllSystems(Set<String> remoteSystems, File fileToCopy, String destination) {
        ExecutorService pool = Executors.newFixedThreadPool(5);

        List<Future<String>> futures = new ArrayList<>();

        for (String remoteSystem:remoteSystems) {
            futures.add(pool.submit(new ScpFileToOneSystem(remoteSystem, fileToCopy, destination)));
        }

        for(Future<String> future : futures){
            try {
                String result = future.get();
                System.out.println(result);
            } catch (InterruptedException e) {
                System.out.println("Interrupted!");
            } catch (Exception e) {
                System.out.println("Exception!" + e.getMessage());
                future.cancel(true);
            }
        }
        pool.shutdown();

    }

    public static void main( String[] args )
    {
        String usageHint = "Usage: scpall <alias from /etc/clusters> <file to scp to those systems> <optional path to write the file on the remote system>.\n\n" +
                "If path is included -\n" +
                "   A slash at the start will be a complete path\n" +
                "   No slash at the start will be taken as a path under the home directory\n" +
                "   End with a filename to specify the destination file name, otherwise it will stay the same.\n\n" +
                "Example line in /etc/clusters:\n" +
                "   nifistage ubuntu@nifi-1.east.usermind.com ubuntu@nifi-2.east.usermind.com ubuntu@nifi-3.east.usermind.com";
        //Now, was a file specified?
        if (args.length < 2 || args.length > 3 || StringUtils.isEmpty(args[0]) || StringUtils.isEmpty(args[1])) {
            System.out.println( usageHint );
            return;
        }

        //First, read in the cluster file and get the system names.
        Set<String> remoteSystems = readClusterFile(args[0]);

        File fileToCopy = new File(args[1]);
        if(!fileToCopy.exists()) {
            System.out.println( "File not found: " + args[1] );
        }

        String destination = "";
        if (args.length > 2) {
            destination = args[2];
            if (StringUtils.isBlank(destination))
                destination = "";
        }

        //Now walk each system and scp the file up to it.
        scpTheFileToAllSystems(remoteSystems, fileToCopy, destination);
    }

}
