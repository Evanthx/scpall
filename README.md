Build this with Maven, and then add this alias:

function scpall { java -jar ~/projects/scpall/target/scpall-1.0.jar $1 $2 }

Create a file called /etc/clusters similar to this:

groupOne user@systemname user@systemname2 user@systemname3

THen you can just use the alias - scpall group2 filename

