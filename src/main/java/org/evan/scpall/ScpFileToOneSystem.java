package org.evan.scpall;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;


public class ScpFileToOneSystem implements Callable<String> {

    private String remoteSystem;
    private File fileToCopy;
    private String destination;

    public ScpFileToOneSystem(String remoteSystem, File fileToCopy, String destination) {
        this.remoteSystem = remoteSystem;
        this.fileToCopy = fileToCopy;
        this.destination = destination;
    }

    public String call() throws Exception {
        System.out.println("SCP starting to " + remoteSystem);
        String returnMessage = "SCP to " + remoteSystem + " is done";

        try {
            String[] cmd = {
                    "/bin/zsh",
                    "-c",
                    buildTheCommand(remoteSystem, fileToCopy, destination)
            };

            Process p = Runtime.getRuntime().exec(cmd);

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            p.waitFor();

            while (br.ready()) {
                String str = br.readLine();
                System.out.println(str);
            }

        } catch (Exception e) {
            System.out.println("Failed for the remoteSystem " + remoteSystem + ": " + e.getMessage());
        }
        return returnMessage;
    }

    public static String buildTheCommand(String remoteSystem, File fileToCopy, String destination) {
        StringBuilder sb = new StringBuilder();

        sb.append("source ~/.zshrc; echo Starting ");
        sb.append(remoteSystem);
        sb.append("; scp -i ~/.ssh/aws.pem ");
        sb.append(fileToCopy.getAbsoluteFile());
        sb.append(" ");
        sb.append(remoteSystem);
        sb.append(":");
        sb.append(destination);
        sb.append("; echo Finished ");
        sb.append(remoteSystem);

        return sb.toString();
    }

}
