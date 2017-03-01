***********************************************************
* Hydrant
* Copyright(c) 2014 kikisoftware All rights Reserved.
***********************************************************

* This is a java-application. It requires Java

Hydrant is a reverse proxy server that relay the network.
Hydrant can controll a speed of network.
you can choose a download speed including no limit.


[Install]
It's just unzip and save it to local directory.

[Setting]
First, you have to change just 2 topic on conf/hydrant.properties

1. You have to correct "hostPort" as the server ip-address and port-number.
2. You may change requestRule. It's a URL Rewrite Rule. You could read the explanation, and write by your environment.

Next, open a starter script (if your environment is windows then start.bat else start.sh) and edit "JAVA_EXE" the path to java.exe.

[Invocation]
execute the starter script.(if your environment is windows then start.bat else start.sh)

[Summarize Access Log]
1. open a starter script (if your environment is windows then sumaccess.bat else sumaccess.sh) and edit "JAVA_EXE" the path to java.exe.
2. execute the starter script.(if your environment is windows then sumaccess.bat else sumaccess.sh)
3. It output a Tab-Separate-Value file "accessSum.tsv" on current directory (default setting).
